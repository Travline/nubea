package com.ecommerce.backend.inventario.service;

import com.ecommerce.backend.inventario.dto.ActualizarInventarioRequest;
import com.ecommerce.backend.inventario.dto.InventarioActualResponse;
import com.ecommerce.backend.shared.exception.ConflictoCampoException;
import com.ecommerce.backend.shared.exception.MensajesUsuario;
import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import com.ecommerce.backend.shared.exception.ValidacionCampoException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modificaciones manuales de inventario (RN-M3-10/11).
 *
 * <p>{@code inventario.stock_actual} guarda las unidades DISPONIBLES: el checkout las descuenta al crear el
 * pedido (reserva ACTIVA) y {@code trg_pedido_transicion} las devuelve si el pedido se cancela (reserva LIBERADA).
 * Las unidades apartadas son solo las de pedidos pendientes de pago: mientras no exista el módulo de envíos,
 * un pedido PAGADO se considera entregado.
 *
 * <p>Todas las operaciones bloquean la fila de inventario ({@code FOR UPDATE}); el checkout y el trigger
 * actualizan esa misma fila, por lo que la suma de apartadas leída después del bloqueo es consistente.
 */
@Service
@RequiredArgsConstructor
public class AjusteInventarioService {
    private final JdbcTemplate jdbc;

    private record Fila(int stockActual, int stockMinimo) {}

    /** Ajuste relativo (+/−) del día a día. No permite quedar en negativo. */
    @Transactional
    public InventarioActualResponse ajustar(UUID idTienda, UUID idPresentacion, int cantidad) {
        if (cantidad == 0) throw new ValidacionCampoException("cantidad", MensajesUsuario.AJUSTE_CERO);
        Fila fila = bloquear(idTienda, idPresentacion);
        int resultado = fila.stockActual() + cantidad;
        if (resultado < 0) {
            throw new ConflictoCampoException("cantidad", MensajesUsuario.soloQuedanUnidades(fila.stockActual()));
        }
        jdbc.update("UPDATE inventario SET stock_actual = ?, fecha_actualizacion = now() WHERE id_presentacion = ?",
                resultado, idPresentacion);
        return new InventarioActualResponse(idPresentacion, resultado, fila.stockMinimo(), apartadas(idPresentacion));
    }

    /**
     * "Fijar stock" tras un conteo: {@code unidadesEnMano} es lo que el vendedor contó. Se guardan como disponibles
     * {@code unidadesEnMano − apartadas}; así, si un pedido pendiente se cancela y el trigger devuelve sus
     * unidades, el stock no queda inflado.
     */
    @Transactional
    public InventarioActualResponse fijar(UUID idTienda, UUID idPresentacion, ActualizarInventarioRequest request) {
        Fila fila = bloquear(idTienda, idPresentacion);
        if (request.stockEsperado() != null && request.stockEsperado() != fila.stockActual()) {
            throw new ConflictoCampoException("stockEsperado", MensajesUsuario.STOCK_CAMBIO);
        }
        int apartadas = apartadas(idPresentacion);
        if (request.stockActual() < apartadas) {
            throw new ConflictoCampoException("stockActual", MensajesUsuario.unidadesApartadas(apartadas));
        }
        int disponibles = request.stockActual() - apartadas;
        jdbc.update("UPDATE inventario SET stock_actual = ?, stock_minimo = ?, fecha_actualizacion = now() WHERE id_presentacion = ?",
                disponibles, request.stockMinimo(), idPresentacion);
        return new InventarioActualResponse(idPresentacion, disponibles, request.stockMinimo(), apartadas);
    }

    private Fila bloquear(UUID idTienda, UUID idPresentacion) {
        return jdbc.query("""
                SELECT i.stock_actual, i.stock_minimo FROM inventario i
                JOIN presentacion_producto p ON p.id_presentacion = i.id_presentacion
                WHERE p.id_tienda = ? AND i.id_presentacion = ?
                FOR UPDATE OF i
                """, (rs, n) -> new Fila(rs.getInt(1), rs.getInt(2)), idTienda, idPresentacion)
                .stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos el inventario de esa versión en tu tienda"));
    }

    private int apartadas(UUID idPresentacion) {
        Integer total = jdbc.queryForObject("""
                SELECT COALESCE(sum(pi.cantidad), 0)::int FROM pedido_item pi
                JOIN pedido pe ON pe.id_pedido = pi.id_pedido
                WHERE pi.id_presentacion = ? AND pe.estado_reserva = 'ACTIVA'
                """, Integer.class, idPresentacion);
        return total == null ? 0 : total;
    }
}
