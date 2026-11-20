package com.ecommerce.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {"mercadopago.notificaciones.enabled=true", "mercadopago.notificaciones.secret=clave-prueba"})
@AutoConfigureMockMvc
@Transactional
class NotificacionOrderIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    private static final String FIRMA = "ts=1700000000,v1=ea45cb42eb8500f79ad9371c021e6a346f935319568fde7ccf4a2387fc50ee7e";

    @Test
    void validaFirmaSinSesionNiCsrfYEncolaSinConfirmarPago() throws Exception {
        String ruta = "/api/publico/mercadopago/notificaciones?data.id=ORD123&type=order";
        mvc.perform(post(ruta).header("x-request-id", "req-123").header("x-signature", "invalida"))
                .andExpect(status().isUnauthorized());
        for (int i = 0; i < 2; i++)
            mvc.perform(post(ruta).header("x-request-id", "req-123").header("x-signature", FIRMA))
                    .andExpect(status().isOk());
        assertEquals(2L, jdbc.queryForObject("SELECT version FROM notificacion_order_mercadopago WHERE id_order='ORD123'", Long.class));
        assertEquals(0L, jdbc.queryForObject("SELECT version_procesada FROM notificacion_order_mercadopago WHERE id_order='ORD123'", Long.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM pago", Integer.class));
    }
}
