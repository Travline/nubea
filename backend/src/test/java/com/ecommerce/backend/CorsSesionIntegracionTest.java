package com.ecommerce.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "app.cors.allowed-origins=https://cms.example.test",
        "server.servlet.session.cookie.secure=true",
        "server.servlet.session.cookie.same-site=none"
})
@AutoConfigureMockMvc
@Transactional
class CorsSesionIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired org.springframework.session.jdbc.JdbcIndexedSessionRepository sesiones;

    @Test
    void sesionEntreOrigenesUsaCorsYCookieCsrf() throws Exception {
        String origen = "https://cms.example.test";
        mvc.perform(options("/api/auth/login").header("Origin", origen)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origen))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        var csrf = mvc.perform(get("/api/auth/csrf").header("Origin", origen))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origen))
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(csrf.getResponse().getContentAsString(), "$.token");
        var cookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(cookie);
        org.junit.jupiter.api.Assertions.assertTrue(cookie.getSecure());
        org.junit.jupiter.api.Assertions.assertEquals("none", cookie.getAttribute("SameSite"));
        mvc.perform(post("/api/auth/registro").header("Origin", origen).cookie(cookie)
                .header("X-XSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombres\":\"Prueba CORS\",\"correoElectronico\":\"cors@example.test\",\"password\":\"password-seguro\"}"))
                .andExpect(status().isCreated());
        var login = mvc.perform(post("/api/auth/login").header("Origin", origen).cookie(cookie)
                .header("X-XSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"cors@example.test\",\"password\":\"password-seguro\"}"))
                .andExpect(status().isNoContent()).andReturn();
        var session = login.getResponse().getCookie("SESSION");
        org.junit.jupiter.api.Assertions.assertNotNull(session);
        org.junit.jupiter.api.Assertions.assertEquals(-1, session.getMaxAge());
        String idSesion = new String(java.util.Base64.getDecoder().decode(session.getValue()), java.nio.charset.StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertEquals(java.time.Duration.ofHours(2), ((org.springframework.session.Session) sesiones.findById(idSesion)).getMaxInactiveInterval());
        mvc.perform(get("/api/auth/me").header("Origin", origen).cookie(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correoElectronico").value("cors@example.test"));
        mvc.perform(post("/logout").header("Origin", origen).cookie(session).cookie(cookie)
                .header("X-XSRF-TOKEN", token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tiendas/propietario/00000000-0000-0000-0000-000000000001").header("Origin", origen))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recordarAplicaLimiteAbsolutoYCookiePersistente() throws Exception {
        var csrf = mvc.perform(get("/api/auth/csrf")).andReturn();
        var csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        String token = com.jayway.jsonpath.JsonPath.read(csrf.getResponse().getContentAsString(), "$.token");
        mvc.perform(post("/api/auth/registro").cookie(csrfCookie).header("X-XSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombres\":\"Prueba\",\"correoElectronico\":\"recordar@example.test\",\"password\":\"password-seguro\"}"))
                .andExpect(status().isCreated());
        var login = mvc.perform(post("/api/auth/login").cookie(csrfCookie).header("X-XSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"recordar@example.test\",\"password\":\"password-seguro\",\"recordarme\":true}"))
                .andExpect(status().isNoContent()).andReturn();
        var cookie = login.getResponse().getCookie("SESSION");
        org.junit.jupiter.api.Assertions.assertNotNull(cookie);
        org.junit.jupiter.api.Assertions.assertEquals(86400, cookie.getMaxAge());
        org.junit.jupiter.api.Assertions.assertTrue(cookie.isHttpOnly());
        String id = new String(java.util.Base64.getDecoder().decode(cookie.getValue()), java.nio.charset.StandardCharsets.UTF_8);
        org.springframework.session.Session guardada = sesiones.findById(id);
        org.junit.jupiter.api.Assertions.assertNotNull(guardada);
        org.junit.jupiter.api.Assertions.assertEquals(java.time.Duration.ofDays(1), guardada.getMaxInactiveInterval());
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isOk());
        guardada = sesiones.findById(id);
        guardada.setAttribute("venceAbsoluto", System.currentTimeMillis() - 1);
        ((org.springframework.session.SessionRepository) sesiones).save(guardada);
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
        org.junit.jupiter.api.Assertions.assertNull(sesiones.findById(id));
    }

}
