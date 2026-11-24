package com.ecommerce.backend.compra;

import com.ecommerce.backend.shared.tienda.TiendasPublicas;

import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import com.ecommerce.backend.shared.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PedidosController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider actual;
    /** {@code numero}: número corto del pedido en la tienda (#1001, #1002…). */
    public record Pedido(UUID id, String estado, OffsetDateTime fecha, BigDecimal subtotal, BigDecimal impuesto, BigDecimal total, Integer numero) {}
    public record Linea(String producto, String presentacion, int cantidad, BigDecimal precio, BigDecimal total) {}
    public record Detalle(Pedido pedido, String receptor, String telefono, String direccion, String referencia,
                          String departamento, String provincia, String distrito, String placeId, List<Linea> items) {}
    private UUID usuario() { return actual.getCurrentUserId().orElseThrow(() -> new AccessDeniedException("Acceso denegado")); }
    private UUID tienda(String slug) {
        // El comprador consulta sus pedidos aunque la tienda ya no esté disponible (suspendida, cerrada...).
        return TiendasPublicas.existente(jdbc, slug);
    }
    @GetMapping("/api/compra/tiendas/{slug}/pedidos")
    @Transactional(readOnly=true)
    public List<Pedido> misPedidos(@PathVariable String slug) { return listar(tienda(slug),usuario()); }
    @GetMapping("/api/compra/tiendas/{slug}/pedidos/{id}")
    @Transactional(readOnly=true)
    public Detalle miPedido(@PathVariable String slug,@PathVariable UUID id) { return detalle(tienda(slug),usuario(),id); }
    @GetMapping("/api/tiendas/{idTienda}/pedidos")
    @PreAuthorize("@tiendaAuthorizationService.puedeVer(#idTienda, authentication)")
    @Transactional(readOnly=true)
    public List<Pedido> pedidosTienda(@PathVariable UUID idTienda) { return listar(idTienda,null); }
    @GetMapping("/api/tiendas/{idTienda}/pedidos/{id}")
    @PreAuthorize("@tiendaAuthorizationService.puedeVer(#idTienda, authentication)")
    @Transactional(readOnly=true)
    public Detalle pedidoTienda(@PathVariable UUID idTienda,@PathVariable UUID id) { return detalle(idTienda,null,id); }
    private List<Pedido> listar(UUID tienda,UUID usuario) {
        return jdbc.query("SELECT id_pedido,estado,fecha_creacion,subtotal,monto_impuesto,total,numero FROM pedido WHERE id_tienda=? AND (?::uuid IS NULL OR id_usuario=?) ORDER BY fecha_creacion DESC,numero DESC LIMIT 100",
            (r,n)->new Pedido(r.getObject(1,UUID.class),r.getString(2),r.getObject(3,OffsetDateTime.class),r.getBigDecimal(4),r.getBigDecimal(5),r.getBigDecimal(6),r.getInt(7)),tienda,usuario,usuario);
    }
    private Detalle detalle(UUID tienda,UUID usuario,UUID id) {
        var detalles=jdbc.query("""
            SELECT id_pedido,estado,fecha_creacion,subtotal,monto_impuesto,total,nombre_receptor,telefono_receptor,
                   direccion_entrega,referencia_entrega,departamento_entrega,provincia_entrega,distrito_entrega,place_id_entrega,numero
            FROM pedido WHERE id_tienda=? AND (?::uuid IS NULL OR id_usuario=?) AND id_pedido=?
            """,(r,n)->new Detalle(new Pedido(r.getObject(1,UUID.class),r.getString(2),r.getObject(3,OffsetDateTime.class),r.getBigDecimal(4),r.getBigDecimal(5),r.getBigDecimal(6),r.getInt(15)),r.getString(7),r.getString(8),r.getString(9),r.getString(10),r.getString(11),r.getString(12),r.getString(13),r.getString(14),List.of()),tienda,usuario,usuario,id);
        if (detalles.isEmpty()) throw new RecursoNoEncontradoException("Pedido no encontrado");
        var d=detalles.getFirst();
        var items=jdbc.query("SELECT nombre_producto,descripcion_presentacion,cantidad,precio_unitario,total_linea FROM pedido_item WHERE id_pedido=? ORDER BY id_pedido_item",
                (r,n)->new Linea(r.getString(1),r.getString(2),r.getInt(3),r.getBigDecimal(4),r.getBigDecimal(5)),id);
        return new Detalle(d.pedido(),d.receptor(),d.telefono(),d.direccion(),d.referencia(),d.departamento(),d.provincia(),d.distrito(),d.placeId(),items);
    }
}
