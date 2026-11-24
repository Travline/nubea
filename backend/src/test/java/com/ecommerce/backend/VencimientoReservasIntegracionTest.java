package com.ecommerce.backend;

import static com.ecommerce.backend.CompraFixture.importe;
import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.backend.compra.CheckoutService;
import com.ecommerce.backend.compra.ReservasPedido;
import com.ecommerce.backend.compra.VencimientoReservasService;
import com.ecommerce.backend.compra.VencimientoReservasService.Resultado;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Bloque 3, puntos 1 y 3: vencimiento de reservas independiente de Mercado Pago y devolución al carrito. */
@SpringBootTest
@Transactional
class VencimientoReservasIntegracionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired CheckoutService checkout;
    @Autowired ReservasPedido reservas;
    @Autowired TransactionTemplate tx;
    @Autowired VencimientoReservasService vencimientos; // en los tests, mercadopago.pagos.enabled=false

    private CompraFixture f;
    private UUID version;

    @BeforeEach
    void preparar() {
        f = new CompraFixture(jdbc);
        version = f.version(f.producto("PUBLICADO"), "ACTIVA", 10);
    }

    /** Checkout real de 2 unidades: stock 10 → 8, carrito cerrado, intento PREPARADO. */
    private UUID pedido() {
        f.alCarrito(version, 2);
        return checkout.reservarIdempotente(f.comprador, f.slug, f.direccion,
                "checkout_" + UUID.randomUUID().toString().replace("-", ""), importe("23.60")).idPedido();
    }

    private void vencidoHace(UUID pedido, int minutos) {
        jdbc.update("UPDATE pedido SET fecha_expiracion_reserva = now() - (? * interval '1 minute') WHERE id_pedido=?", minutos, pedido);
    }

    private void intento(UUID pedido, String estado, String order) {
        jdbc.update("UPDATE intento_order_mercadopago SET estado=?, id_order=?, cuenta_receptora='cta' WHERE id_pedido=?", estado, order, pedido);
    }

    private String estado(UUID pedido) {
        return jdbc.queryForObject("SELECT estado FROM pedido WHERE id_pedido=?", String.class, pedido);
    }

    @Test
    void sinOrdenEnMercadoPagoSeCancelaAlVencerYVuelveAlCarrito() {
        UUID pedido = pedido();
        assertEquals(8, f.stock(version));
        assertNull(f.carritoActivo());

        assertEquals(Resultado.SIN_CAMBIOS, vencimientos.procesar(pedido), "No vencido: no se toca");
        vencidoHace(pedido, 1);
        assertEquals(Resultado.CANCELADO, vencimientos.procesar(pedido));
        assertEquals("CANCELADO", estado(pedido));
        assertEquals("LIBERADA", jdbc.queryForObject("SELECT estado_reserva FROM pedido WHERE id_pedido=?", String.class, pedido));
        assertEquals(10, f.stock(version), "El stock vuelve una sola vez");
        assertEquals(2, f.cantidadEnCarrito(version), "Los productos vuelven a un carrito activo nuevo");
        assertEquals(Resultado.SIN_CAMBIOS, vencimientos.procesar(pedido), "Idempotente");
        assertEquals(10, f.stock(version));
    }

    @Test
    void creacionInciertaEsperaLaGraciaAntesDeCancelar() {
        UUID pedido = pedido();
        intento(pedido, "CREACION_INCIERTA", null);
        vencidoHace(pedido, 10);
        assertEquals(Resultado.EN_GRACIA, vencimientos.procesar(pedido));
        assertEquals("PENDIENTE_PAGO", estado(pedido));
        vencidoHace(pedido, 41);
        assertEquals(Resultado.CANCELADO, vencimientos.procesar(pedido));
        assertEquals(10, f.stock(version));
    }

    @Test
    void ordenCreadaLaResuelveElProcesadorSiEstaActivo() {
        UUID pedido = pedido();
        intento(pedido, "CREADA", "ORD-" + pedido);
        vencidoHace(pedido, 120);
        var conProcesador = new VencimientoReservasService(jdbc, tx, reservas, true, 40);
        assertEquals(Resultado.DEL_PROCESADOR, conProcesador.procesar(pedido));
        assertEquals("PENDIENTE_PAGO", estado(pedido));

        // Procesador apagado: la orden ya expiró en Mercado Pago (PT30M), se cancela pasada la gracia.
        vencidoHace(pedido, 20);
        assertEquals(Resultado.EN_GRACIA, vencimientos.procesar(pedido));
        vencidoHace(pedido, 41);
        assertEquals(Resultado.CANCELADO, vencimientos.procesar(pedido));
    }

    @Test
    void unTrabajoDePagoEnCursoNoSeInterrumpe() {
        UUID pedido = pedido();
        intento(pedido, "PREPARADO", null);
        jdbc.update("UPDATE intento_order_mercadopago SET tarea_dueno=?, tarea_hasta=now()+interval '2 minutes' WHERE id_pedido=?", UUID.randomUUID(), pedido);
        vencidoHace(pedido, 5);
        assertEquals(Resultado.OCUPADO, vencimientos.procesar(pedido));
        assertEquals("PENDIENTE_PAGO", estado(pedido));
    }

    @Test
    void devolverAlCarritoSumaCantidadesConTopeYFuncionaConLaTiendaSuspendida() {
        UUID primero = pedido();
        f.alCarrito(version, 1);                      // el comprador volvió a agregar 1
        vencidoHace(primero, 1);
        vencimientos.procesar(primero);
        assertEquals(3, f.cantidadEnCarrito(version), "1 que tenía + 2 del pedido vencido");

        UUID segundo = pedido();                       // compra 2 (stock 10 → 8)
        f.alCarrito(version, 998);
        jdbc.update("UPDATE tienda SET estado='SUSPENDIDA' WHERE id_tienda=?", f.tienda);
        vencidoHace(segundo, 1);
        assertEquals(Resultado.CANCELADO, vencimientos.procesar(segundo));
        assertEquals(999, f.cantidadEnCarrito(version), "Tope de 999 por línea");
    }

    @Test
    void otrasCancelacionesNoDevuelvenAlCarrito() {
        UUID pedido = pedido();
        assertEquals(Boolean.TRUE, tx.<Boolean>execute(s -> reservas.cancelar(pedido, false)));   // p. ej. pago rechazado
        assertEquals("CANCELADO", estado(pedido));
        assertEquals(10, f.stock(version));
        assertNull(f.carritoActivo());
        assertEquals(Boolean.FALSE, tx.<Boolean>execute(s -> reservas.cancelar(pedido, true)), "Un pedido ya cancelado no se vuelve a cancelar");
        assertNull(f.carritoActivo());
    }
}
