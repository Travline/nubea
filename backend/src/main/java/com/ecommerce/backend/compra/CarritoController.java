package com.ecommerce.backend.compra;

import com.ecommerce.backend.shared.tienda.TiendasPublicas;

import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import com.ecommerce.backend.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/compra/tiendas/{slug}/carrito")
public class CarritoController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider usuarioActual;
    public record Linea(UUID idPresentacion, UUID idProducto, String producto, String opciones, int cantidad, BigDecimal precio, boolean disponible, boolean valida) {}
    /**
     * {@code total}: suma de todas las líneas (como antes). Nuevos, solo líneas disponibles y válidas
     * (lo que se cobrará en el checkout, con el mismo cálculo): {@code subtotal} sin IGV, {@code igv},
     * {@code tasaIgv} (null si no hay tasa vigente) y {@code totalAPagar}.
     */
    public record Carrito(List<Linea> items, BigDecimal total, BigDecimal subtotal, BigDecimal igv,
                          BigDecimal tasaIgv, BigDecimal totalAPagar) {}
    public record Cantidad(@NotNull @Min(1) @Max(999) Integer cantidad) {}
    private UUID usuario() { return usuarioActual.getCurrentUserId().orElseThrow(() -> new AccessDeniedException("Acceso denegado")); }
    private UUID tienda(String slug) {
        return TiendasPublicas.activa(jdbc, slug);
    }
    @GetMapping
    @Transactional(readOnly=true)
    public Carrito obtener(@PathVariable String slug) { return leer(tienda(slug), usuario()); }
    @PutMapping("/items/{presentacion}")
    @Transactional
    public Carrito guardar(@PathVariable String slug, @PathVariable UUID presentacion, @Valid @RequestBody Cantidad cuerpo) {
        return guardarCantidad(slug,presentacion,cuerpo.cantidad(),false);
    }
    @PostMapping("/items/{presentacion}")
    @Transactional
    public Carrito agregar(@PathVariable String slug, @PathVariable UUID presentacion, @Valid @RequestBody Cantidad cuerpo) {
        return guardarCantidad(slug,presentacion,cuerpo.cantidad(),true);
    }
    private Carrito guardarCantidad(String slug, UUID presentacion, int cantidad, boolean sumar) {
        UUID tienda=tienda(slug), usuario=usuario();
        jdbc.queryForObject("SELECT id_usuario FROM usuario_plataforma WHERE id_usuario=? FOR UPDATE", UUID.class, usuario);
        int valido = jdbc.queryForObject("""
            SELECT count(*) FROM presentacion_producto pr JOIN producto p ON p.id_producto=pr.id_producto AND p.id_tienda=pr.id_tienda
            JOIN categoria c ON c.id_categoria=p.id_categoria AND c.id_tienda=p.id_tienda
            WHERE pr.id_presentacion=? AND pr.id_tienda=? AND pr.estado='ACTIVA' AND p.estado='PUBLICADO' AND c.activa=true
            """,Integer.class,presentacion,tienda);
        if (valido == 0) throw new ReglaNegocioException("La presentación no está disponible en esta tienda");
        jdbc.update("INSERT INTO tienda_cliente(id_usuario,id_tienda) VALUES (?,?) ON CONFLICT DO NOTHING",usuario,tienda);
        jdbc.update("INSERT INTO carrito(id_carrito,id_usuario,id_tienda) VALUES (?,?,?) ON CONFLICT (id_usuario,id_tienda) WHERE estado='ACTIVO' DO NOTHING",UUID.randomUUID(),usuario,tienda);
        UUID carrito=jdbc.queryForObject("SELECT id_carrito FROM carrito WHERE id_usuario=? AND id_tienda=? AND estado='ACTIVO' FOR UPDATE",UUID.class,usuario,tienda);
        if (sumar) {
            var actuales=jdbc.query("SELECT cantidad FROM carrito_item WHERE id_carrito=? AND id_presentacion=?",(r,n)->r.getInt(1),carrito,presentacion);
            if (!actuales.isEmpty()) cantidad += actuales.getFirst();
            if (cantidad > 999) throw new ReglaNegocioException("La cantidad máxima es 999");
        }
        jdbc.update("INSERT INTO carrito_item(id_carrito,id_presentacion,cantidad) VALUES (?,?,?) ON CONFLICT (id_carrito,id_presentacion) DO UPDATE SET cantidad=excluded.cantidad",carrito,presentacion,cantidad);
        jdbc.update("UPDATE carrito SET fecha_actualizacion=now() WHERE id_carrito=?",carrito);
        return leer(tienda,usuario);
    }
    @DeleteMapping("/items/{presentacion}")
    @Transactional
    public Carrito quitar(@PathVariable String slug,@PathVariable UUID presentacion) {
        UUID tienda=tienda(slug), usuario=usuario();
        jdbc.queryForObject("SELECT id_usuario FROM usuario_plataforma WHERE id_usuario=? FOR UPDATE", UUID.class, usuario);
        jdbc.update("DELETE FROM carrito_item WHERE id_presentacion=? AND id_carrito IN (SELECT id_carrito FROM carrito WHERE id_usuario=? AND id_tienda=? AND estado='ACTIVO')",presentacion,usuario,tienda);
        return leer(tienda,usuario);
    }
    private Carrito leer(UUID tienda,UUID usuario) {
        var items=jdbc.query("""
            SELECT pr.id_presentacion,p.id_producto,p.nombre,
              COALESCE((SELECT string_agg(v.nombre || ': ' || vv.valor, ', ' ORDER BY v.nombre) FROM presentacion_valor pv
                JOIN variante v ON v.id_variante=pv.id_variante JOIN valor_variante vv ON vv.id_valor=pv.id_valor WHERE pv.id_presentacion=pr.id_presentacion),''),
              ci.cantidad,COALESCE(pr.precio_override,p.precio_base),COALESCE(i.stock_actual,0)>=ci.cantidad,
              (pr.estado='ACTIVA' AND p.estado='PUBLICADO' AND c.activa=true)
            FROM carrito ca JOIN carrito_item ci ON ci.id_carrito=ca.id_carrito
              JOIN presentacion_producto pr ON pr.id_presentacion=ci.id_presentacion AND pr.id_tienda=ca.id_tienda
              JOIN producto p ON p.id_producto=pr.id_producto AND p.id_tienda=pr.id_tienda
              JOIN categoria c ON c.id_categoria=p.id_categoria AND c.id_tienda=p.id_tienda
              LEFT JOIN inventario i ON i.id_presentacion=pr.id_presentacion
            WHERE ca.id_usuario=? AND ca.id_tienda=? AND ca.estado='ACTIVO' ORDER BY ci.fecha_agregado,pr.id_presentacion
            """,(r,n)->new Linea(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),r.getString(4),r.getInt(5),r.getBigDecimal(6),r.getBoolean(7),r.getBoolean(8)),usuario,tienda);
        BigDecimal total=items.stream().map(i->i.precio().multiply(BigDecimal.valueOf(i.cantidad()))).reduce(BigDecimal.ZERO,BigDecimal::add);
        var comprables=items.stream().filter(i->i.disponible() && i.valida()).toList();
        var impuesto=CalculoImportes.impuestoVigente(jdbc);
        if (impuesto.isEmpty()) {
            BigDecimal aPagar=comprables.stream().map(i->i.precio().multiply(BigDecimal.valueOf(i.cantidad()))).reduce(BigDecimal.ZERO,BigDecimal::add);
            return new Carrito(items,total,null,null,null,aPagar);
        }
        var totales=CalculoImportes.sumar(comprables.stream()
                .map(i->CalculoImportes.linea(i.precio(),i.cantidad(),impuesto.get().tasa())).toList());
        return new Carrito(items,total,totales.subtotal(),totales.impuesto(),impuesto.get().tasa(),totales.total());
    }
}
