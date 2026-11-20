package com.ecommerce.backend.compra;

import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consume exclusivamente órdenes consultadas por el servidor, nunca datos del navegador o webhook. */
@Service
@RequiredArgsConstructor
public class ConfirmacionPagoService {
    private final JdbcTemplate jdbc;
    private record Pedido(UUID id, String estado, BigDecimal total, String vendedor, String intento) {}

    @Transactional
    public boolean confirmar(MercadoPagoOrders.OrdenConsultada orden) {
        if (orden == null || orden.id() == null) throw new ReglaNegocioException("Orden no válida");
        var filas = jdbc.query("""
            SELECT p.id_pedido,p.estado,p.total,c.id_cuenta_externa,i.estado
              FROM intento_order_mercadopago i JOIN pedido p ON p.id_pedido=i.id_pedido
              JOIN cuenta_pago_tienda c ON c.id_tienda=p.id_tienda
             WHERE i.id_order=? AND i.ambiente_pago='PRUEBA' AND p.ambiente_pago='PRUEBA'
             FOR UPDATE OF p,i,c
            """, (r,n) -> new Pedido(r.getObject(1,UUID.class), r.getString(2), r.getBigDecimal(3), r.getString(4), r.getString(5)), orden.id());
        if (filas.size() != 1) throw new ReglaNegocioException("Orden no asociada a una prueba");
        var pedido = filas.getFirst();
        if (!pedido.id().toString().equals(orden.referencia()) || !java.util.Objects.equals(pedido.vendedor(), orden.vendedor())
                || !"PEN".equals(orden.moneda()) || orden.total() == null || pedido.total().compareTo(orden.total()) != 0)
            throw new ReglaNegocioException("La orden no coincide con el pedido");
        if (!"processed".equals(orden.estado()) || !"accredited".equals(orden.detalle())
                || orden.totalPagado() == null || pedido.total().compareTo(orden.totalPagado()) != 0) return false;
        if ("PAGADO".equals(pedido.estado())) return true;
        if (!"PENDIENTE_PAGO".equals(pedido.estado()) || !"CREADA".equals(pedido.intento()))
            throw new ReglaNegocioException("La orden requiere revisión antes de confirmar el pago");
        jdbc.update("""
            INSERT INTO pago(id_pago,id_pedido,referencia_externa,clave_idempotencia,estado,monto,fecha_confirmacion)
            VALUES (?,?,?,?,'APROBADO',?,now())
            """, UUID.randomUUID(),pedido.id(),orden.id(),"order:"+orden.id(),pedido.total());
        jdbc.update("UPDATE pedido SET estado='PAGADO',estado_reserva='CONSUMIDA',fecha_pago=now() WHERE id_pedido=?",pedido.id());
        jdbc.update("UPDATE intento_order_mercadopago SET estado='FINALIZADA',fecha_actualizacion=now() WHERE id_pedido=?",pedido.id());
        return true;
    }
}
