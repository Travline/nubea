package com.ecommerce.backend.compra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class NotificacionOrderControllerTest {
    private static final String FIRMA = "ts=1700000000,v1=ea45cb42eb8500f79ad9371c021e6a346f935319568fde7ccf4a2387fc50ee7e";

    @Test
    void aceptaSoloFirmaValidaDeOrderYGuardaTrabajoDurable() {
        var jdbc = mock(JdbcTemplate.class);
        var controlador = new NotificacionOrderController(jdbc, "clave-prueba");
        assertEquals(HttpStatus.OK, controlador.recibir("ORD123", "order", FIRMA, "req-123").getStatusCode());
        verify(jdbc).update(contains("INSERT INTO notificacion_order_mercadopago"), eq("ORD123"));
        assertEquals(HttpStatus.UNAUTHORIZED, controlador.recibir("ORD124", "order", FIRMA, "req-123").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controlador.recibir("ORD123", "payment", FIRMA, "req-123").getStatusCode());
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void requiereSecretoConfigurado() {
        assertThrows(IllegalStateException.class, () -> new NotificacionOrderController(mock(JdbcTemplate.class), ""));
    }
}
