package com.ecommerce.backend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ecommerce.backend.shared.exception.MensajesUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Bloque 5: sin GOOGLE_CLIENT_ID la aplicación arranca y el inicio con Google responde 503. */
@SpringBootTest(properties = "google.client-id=")
@AutoConfigureMockMvc
class GoogleNoConfiguradoIntegracionTest {
    @Autowired MockMvc mvc;

    @Test
    void sinClientIdElEndpointNoEstaDisponible() throws Exception {
        mvc.perform(post("/api/auth/google").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"x.y.z\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("GOOGLE_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.mensaje").value(MensajesUsuario.GOOGLE_NO_DISPONIBLE));
    }
}
