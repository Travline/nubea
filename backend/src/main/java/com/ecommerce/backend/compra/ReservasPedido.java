package com.ecommerce.backend.compra;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Cancelación de pedidos pendientes y liberación de su reserva (RN-M4-19/20).
 *
 * <p>El llamador ya tiene bloqueado el pedido. Orden de bloqueos dentro de la cancelación: carrito → filas de
 * inventario en orden de id → pedido (el trigger devuelve el stock). Es compatible con el checkout
 * (usuario → tienda → cuenta → carrito → inventario), así que no puede haber interbloqueos entre ellos.
 */
@Component
@RequiredArgsConstructor
public class ReservasPedido {
    private final JdbcTemplate jdbc;

    /**
     * Cancela el pedido si sigue PENDIENTE_PAGO. Si {@code porVencimiento}, sus productos vuelven al carrito
     * ACTIVO del comprador en esa tienda (aunque la tienda no esté disponible).
     *
     * @return true si el pedido se canceló ahora
     */
    public boolean cancelar(UUID pedido, boolean porVencimiento) {
        String estado = jdbc.queryForObject("SELECT estado FROM pedido WHERE id_pedido=?", String.class, pedido);
        if (!"PENDIENTE_PAGO".equals(estado)) return false;
        if (porVencimiento) devolverAlCarrito(pedido);
        jdbc.query("""
                SELECT i.id_presentacion FROM inventario i JOIN pedido_item pi ON pi.id_presentacion = i.id_presentacion
                WHERE pi.id_pedido = ? ORDER BY i.id_presentacion FOR UPDATE OF i
                """, (r, n) -> r.getObject(1, UUID.class), pedido);
        jdbc.update("UPDATE pago SET estado='CANCELADO' WHERE clave_idempotencia=? AND estado='PENDIENTE'", "pedido:" + pedido);
        return jdbc.update("UPDATE pedido SET estado='CANCELADO',estado_reserva='LIBERADA' WHERE id_pedido=? AND estado='PENDIENTE_PAGO'", pedido) == 1;
    }

    /** Suma las líneas del pedido al carrito ACTIVO del comprador (lo crea si no existe; tope 999 por línea). */
    void devolverAlCarrito(UUID pedido) {
        var contexto = jdbc.queryForMap("SELECT id_usuario, id_tienda FROM pedido WHERE id_pedido=?", pedido);
        UUID usuario = (UUID) contexto.get("id_usuario"), tienda = (UUID) contexto.get("id_tienda");
        jdbc.update("INSERT INTO carrito(id_carrito,id_usuario,id_tienda) VALUES (?,?,?) ON CONFLICT (id_usuario,id_tienda) WHERE estado='ACTIVO' DO NOTHING",
                UUID.randomUUID(), usuario, tienda);
        UUID carrito = jdbc.queryForObject("SELECT id_carrito FROM carrito WHERE id_usuario=? AND id_tienda=? AND estado='ACTIVO' FOR UPDATE",
                UUID.class, usuario, tienda);
        jdbc.update("""
                INSERT INTO carrito_item(id_carrito,id_presentacion,cantidad)
                SELECT ?, pi.id_presentacion, LEAST(pi.cantidad, 999) FROM pedido_item pi WHERE pi.id_pedido = ?
                ON CONFLICT (id_carrito,id_presentacion) DO UPDATE SET cantidad = LEAST(carrito_item.cantidad + excluded.cantidad, 999)
                """, carrito, pedido);
        jdbc.update("UPDATE carrito SET fecha_actualizacion=now() WHERE id_carrito=?", carrito);
    }
}
