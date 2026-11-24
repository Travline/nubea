package com.ecommerce.backend.compra;

import com.ecommerce.backend.shared.tienda.TiendasPublicas;

import com.ecommerce.backend.cuentapago.oauth.CredencialesVendedorService;
import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import com.ecommerce.backend.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix="mercadopago.pagos",name="enabled",havingValue="true")
public class PagosController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider actual;
    private final CredencialesVendedorService credenciales;
    private final MercadoPagoOrders mercado;
    private final PagosConfiguracion config;
    private final PagosStore store;
    private final PagosProcesador procesador;
    public record Compra(@NotNull UUID idDireccion,@NotNull BigDecimal totalEsperado,
                         @NotNull @Pattern(regexp="[A-Za-z0-9_-]{16,100}") String clave) {}
    public record Devolucion(@NotNull UUID clave,boolean reponerStock) {}
    private UUID usuario() { return actual.getCurrentUserId().orElseThrow(()->new AccessDeniedException("Acceso denegado")); }
    public record Disponibilidad(boolean disponible,String ambiente) {}
    @GetMapping("/api/compra/tiendas/{slug}/checkout")
    public Disponibilidad disponibilidad(@PathVariable String slug) {
        var tiendas=jdbc.query("""
            SELECT t.id_tienda FROM tienda t JOIN cuenta_pago_tienda c ON c.id_tienda=t.id_tienda
            WHERE t.slug=? AND t.estado='ACTIVA' AND c.estado='ACTIVA'
            """,(r,n)->r.getObject(1,UUID.class),slug);
        if (tiendas.isEmpty()) return new Disponibilidad(false,null);
        try { return new Disponibilidad(true,config.ambiente(tiendas.getFirst())); }
        catch (ReglaNegocioException ex) { return new Disponibilidad(false,null); }
    }
    @PostMapping("/api/compra/tiendas/{slug}/checkout")
    public PagosStore.Estado comprar(@PathVariable String slug,@Valid @RequestBody Compra compra) {
        UUID usuario=usuario();
        UUID tienda=TiendasPublicas.activa(jdbc, slug);
        var anteriores=jdbc.query("""
            SELECT id_pedido,id_direccion,total_esperado FROM checkout_idempotencia
            WHERE id_usuario=? AND id_tienda=? AND clave=?
            """,(r,n)->new Object[]{r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getBigDecimal(3)},usuario,tienda,compra.clave());
        if (!anteriores.isEmpty()) {
            var anterior=anteriores.getFirst();
            if (!compra.idDireccion().equals(anterior[1]) || compra.totalEsperado().compareTo((BigDecimal)anterior[2])!=0)
                throw new ReglaNegocioException("La clave de compra ya se usó con otros datos");
            UUID pedido=(UUID)anterior[0];
            procesador.procesar(pedido);
            return store.estado(pedido);
        }
        String ambiente=config.ambiente(tienda);
        var token=credenciales.obtener(tienda);
        if (!ambiente.equals(mercado.verificarCuenta(token.access(),token.cuenta())))
            throw new ReglaNegocioException("La cuenta del vendedor no corresponde al ambiente configurado");
        UUID pedido=store.preparar(usuario,slug,compra.idDireccion(),compra.clave(),compra.totalEsperado(),token.cuenta(),ambiente,mercado.tiposExcluidos(token.access()));
        procesador.procesar(pedido);
        return store.estado(pedido);
    }
    @GetMapping("/api/compra/tiendas/{slug}/pedidos/{pedido}/pago")
    public PagosStore.Estado consultar(@PathVariable String slug,@PathVariable UUID pedido) {
        int visibles=jdbc.queryForObject("SELECT count(*) FROM pedido p JOIN tienda t ON t.id_tienda=p.id_tienda WHERE p.id_pedido=? AND p.id_usuario=? AND t.slug=?",Integer.class,pedido,usuario(),slug);
        if (visibles!=1) throw new RecursoNoEncontradoException("Pedido no encontrado");
        verificarIntento(pedido);
        return store.estado(pedido);
    }
    @GetMapping("/api/tiendas/{idTienda}/pedidos/{pedido}/pago")
    @PreAuthorize("@tiendaAuthorizationService.puedeVer(#idTienda, authentication)")
    public PagosStore.Estado consultarVendedor(@PathVariable UUID idTienda,@PathVariable UUID pedido) {
        verificarTienda(idTienda,pedido); verificarIntento(pedido); return store.estado(pedido);
    }
    @PostMapping("/api/tiendas/{idTienda}/pedidos/{pedido}/reembolsos")
    @PreAuthorize("@tiendaAuthorizationService.puedeVer(#idTienda, authentication)")
    public PagosStore.Estado reembolsar(@PathVariable UUID idTienda,@PathVariable UUID pedido,@Valid @RequestBody Devolucion cuerpo) {
        store.solicitarReembolso(idTienda,pedido,usuario(),cuerpo.clave(),cuerpo.reponerStock());
        procesador.procesar(pedido);
        return store.estado(pedido);
    }
    private void verificarIntento(UUID pedido) {
        if (jdbc.queryForObject("SELECT count(*) FROM intento_order_mercadopago WHERE id_pedido=?",Integer.class,pedido)!=1)
            throw new RecursoNoEncontradoException("Este pedido no tiene una operación de pago integrada");
    }
    private void verificarTienda(UUID tienda,UUID pedido) {
        if (jdbc.queryForObject("SELECT count(*) FROM pedido WHERE id_pedido=? AND id_tienda=?",Integer.class,pedido,tienda)!=1)
            throw new RecursoNoEncontradoException("Pedido no encontrado");
    }
}
