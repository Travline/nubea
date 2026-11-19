package com.ecommerce.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
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
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").header("Origin", origen).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correoElectronico").value("cors@example.test"));
        mvc.perform(post("/logout").header("Origin", origen).session(session).cookie(cookie)
                .header("X-XSRF-TOKEN", token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tiendas/propietario/00000000-0000-0000-0000-000000000001").header("Origin", origen))
                .andExpect(status().isUnauthorized());
    }

}
