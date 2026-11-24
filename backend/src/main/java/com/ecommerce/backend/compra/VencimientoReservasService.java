package com.ecommerce.backend.compra;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Vence las reservas de pedidos PENDIENTE_PAGO, con o sin pagos de Mercado Pago habilitados (RN-M4-19).
 *
 * <p>Coordinación con {@link PagosProcesador}: cada pedido va en su propia transacción y bloquea primero el
 * intento ({@code FOR UPDATE SKIP LOCKED}) y luego el pedido, el mismo orden que {@code PagosStore.bloquear}.
 * Un pedido con un trabajo de pago en curso ({@code tarea_hasta > now()}) se salta.
 * <ul>
 *   <li>Sin intento o intento PREPARADO (no hay orden en Mercado Pago): se cancela al vencer.</li>
 *   <li>CREACION_INCIERTA sin orden: se cancela pasada la gracia. La orden se crea con expiration_time PT30M
 *       y solo antes del vencimiento, así que pasada la gracia ya no se puede pagar; si llega un pago tardío,
 *       el procesador lo reembolsa (RN-M4-21).</li>
 *   <li>Con orden creada: si el procesador está activo, lo resuelve él (cancela y confirma en Mercado Pago);
 *       si está apagado, se cancela pasada la gracia.</li>
 * </ul>
 * Las cancelaciones de este servicio son por vencimiento: los productos vuelven al carrito del comprador.
 */
@Service
public class VencimientoReservasService {
    private static final Logger log = LoggerFactory.getLogger(VencimientoReservasService.class);
    private static final int LOTE = 50;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ReservasPedido reservas;
    private final boolean procesadorActivo;
    private final int graciaMinutos;

    public VencimientoReservasService(JdbcTemplate jdbc, TransactionTemplate tx, ReservasPedido reservas,
            @Value("${mercadopago.pagos.enabled:false}") boolean procesadorActivo,
            @Value("${reservas.vencimiento.gracia-minutos:40}") int graciaMinutos) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.reservas = reservas;
        this.procesadorActivo = procesadorActivo;
        this.graciaMinutos = graciaMinutos;
    }

    public enum Resultado { CANCELADO, EN_GRACIA, DEL_PROCESADOR, OCUPADO, SIN_CAMBIOS }

    private record Intento(String estado, String order, boolean ocupado) {}

    @Scheduled(fixedDelayString = "${reservas.vencimiento.delay-ms:60000}", initialDelayString = "${reservas.vencimiento.delay-ms:60000}")
    public void ejecutar() {
        List<UUID> vencidos = jdbc.query("""
                SELECT id_pedido FROM pedido WHERE estado='PENDIENTE_PAGO' AND fecha_expiracion_reserva < now()
                ORDER BY fecha_expiracion_reserva LIMIT ?
                """, (r, n) -> r.getObject(1, UUID.class), LOTE);
        for (UUID pedido : vencidos) {
            try {
                procesar(pedido);
            } catch (RuntimeException ex) {
                log.warn("No se pudo vencer la reserva del pedido {}; se reintentará", pedido, ex);
            }
        }
    }

    /** Procesa un pedido en su propia transacción (o en la del llamador, si existe). */
    public Resultado procesar(UUID pedido) {
        return tx.execute(estado -> decidir(pedido));
    }

    private Resultado decidir(UUID pedido) {
        boolean tieneIntento = jdbc.queryForObject("SELECT count(*) FROM intento_order_mercadopago WHERE id_pedido=?", Integer.class, pedido) > 0;
        Intento intento = null;
        if (tieneIntento) {
            var filas = jdbc.query("""
                    SELECT estado, id_order, (tarea_hasta IS NOT NULL AND tarea_hasta > now()) FROM intento_order_mercadopago
                    WHERE id_pedido=? FOR UPDATE SKIP LOCKED
                    """, (r, n) -> new Intento(r.getString(1), r.getString(2), r.getBoolean(3)), pedido);
            if (filas.isEmpty() || filas.getFirst().ocupado()) return Resultado.OCUPADO;
            intento = filas.getFirst();
        }
        // Comparaciones de tiempo en la BD: mismo reloj que fecha_expiracion_reserva.
        var actual = jdbc.queryForMap("""
                SELECT estado = 'PENDIENTE_PAGO' AND fecha_expiracion_reserva < now() AS vencido,
                       fecha_expiracion_reserva + (? * interval '1 minute') < now() AS paso_gracia,
                       fecha_expiracion_reserva + interval '24 hours' < now() AS paso_un_dia
                FROM pedido WHERE id_pedido=? FOR UPDATE
                """, graciaMinutos, pedido);
        if (!Boolean.TRUE.equals(actual.get("vencido"))) return Resultado.SIN_CAMBIOS;
        boolean pasoLaGracia = Boolean.TRUE.equals(actual.get("paso_gracia"));
        boolean pasoUnDia = Boolean.TRUE.equals(actual.get("paso_un_dia"));

        if (intento == null || ("PREPARADO".equals(intento.estado()) && intento.order() == null)) {
            return cancelar(pedido);
        }
        if (intento.order() == null && "CREACION_INCIERTA".equals(intento.estado())) {
            return pasoLaGracia ? cancelar(pedido) : Resultado.EN_GRACIA;
        }
        if (intento.order() != null) {
            if (procesadorActivo) {
                if (pasoUnDia) {
                    log.warn("El pedido {} sigue pendiente 24 h después de vencer; revisar la orden {} en Mercado Pago", pedido, intento.order());
                }
                return Resultado.DEL_PROCESADOR;
            }
            return pasoLaGracia ? cancelar(pedido) : Resultado.EN_GRACIA;
        }
        return Resultado.SIN_CAMBIOS;
    }

    private Resultado cancelar(UUID pedido) {
        return reservas.cancelar(pedido, true) ? Resultado.CANCELADO : Resultado.SIN_CAMBIOS;
    }
}
