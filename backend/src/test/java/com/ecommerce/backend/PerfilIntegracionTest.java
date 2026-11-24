package com.ecommerce.backend;

import static com.ecommerce.backend.shared.exception.MensajesUsuario.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Perfil de la propia cuenta: datos, documento compartido con el registro y cambio de contraseña. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerfilIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.ecommerce.backend.identidad.repository.UsuarioPlataformaRepository usuarios;

    private static final String CLAVE = "clave-segura-123";

    private String registrar() throws Exception {
        String correo = "perfil-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombres\":\"Ana\",\"correoElectronico\":\"" + correo + "\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isCreated());
        return correo;
    }

    private ResultActions login(String correo, String clave) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + correo + "\",\"password\":\"" + clave + "\"}"));
    }

    private Cookie sesion(String correo) throws Exception {
        return login(correo, CLAVE).andExpect(status().isNoContent()).andReturn().getResponse().getCookie("SESSION");
    }

    private UUID usuario(String correo) {
        return usuarios.findByEmail(correo).orElseThrow().getIdUsuario(); // JPA vacía antes la caché del registro
    }

    private static Authentication como(UUID id) {
        return UsernamePasswordAuthenticationToken.authenticated(id, null, List.of());
    }

    private ResultActions actualizar(UUID id, String cuerpo) throws Exception {
        return mvc.perform(put("/api/perfil").with(csrf()).with(authentication(como(id)))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions cambiar(Cookie sesion, String actual, String nueva) throws Exception {
        return mvc.perform(put("/api/perfil/contrasena").cookie(sesion).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contrasenaActual\":\"" + actual + "\",\"contrasenaNueva\":\"" + nueva + "\"}"));
    }

    // ---------- seguridad ----------

    @Test
    void soloLaPropiaCuentaConSesionYCsrf() throws Exception {
        mvc.perform(get("/api/perfil")).andExpect(status().isUnauthorized());
        String correo = registrar();
        UUID id = usuario(correo);
        mvc.perform(get("/api/perfil").with(authentication(como(id)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(id.toString()))
                .andExpect(jsonPath("$.correoElectronico").value(correo))
                .andExpect(jsonPath("$.tieneContrasena").value(true))
                .andExpect(jsonPath("$.tieneGoogle").value(false));
        mvc.perform(put("/api/perfil").with(authentication(como(id))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombres\":\"Sin CSRF\"}")).andExpect(status().isForbidden());
    }

    // ---------- datos y documento ----------

    @Test
    void actualizaDatosYDocumento() throws Exception {
        UUID id = usuario(registrar());
        actualizar(id, "{\"nombres\":\" Ana María \",\"apellidoPaterno\":\"Pérez\",\"telefono\":\"+51 999 888 777\",\"tipoDocumento\":\"dni\",\"numeroDocumento\":\"12345678\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres").value("Ana María"))
                .andExpect(jsonPath("$.tipoDocumento").value("DNI"))
                .andExpect(jsonPath("$.paisEmisorDocumento").value("PE"));
        // Sin tipo ni número, el documento se borra
        actualizar(id, "{\"nombres\":\"Ana\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").doesNotExist())
                .andExpect(jsonPath("$.numeroDocumento").doesNotExist());
    }

    @Test
    void erroresDeDocumentoLleganConCampo() throws Exception {
        UUID id = usuario(registrar());
        actualizar(id, "{\"nombres\":\"Ana\",\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"123\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.numeroDocumento").value(DNI_INVALIDO));
        actualizar(id, "{\"nombres\":\"Ana\",\"tipoDocumento\":\"DNI\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.numeroDocumento").value(DOCUMENTO_TIPO_Y_NUMERO));
        actualizar(id, "{\"nombres\":\"Ana\",\"numeroDocumento\":\"12345678\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.tipoDocumento").value(DOCUMENTO_TIPO_Y_NUMERO));
        actualizar(id, "{\"nombres\":\"Ana\",\"tipoDocumento\":\"XX\",\"numeroDocumento\":\"12345678\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.tipoDocumento").value(DOCUMENTO_TIPO_INVALIDO));
        actualizar(id, "{\"nombres\":\"Ana\",\"tipoDocumento\":\"CE\",\"numeroDocumento\":\"12\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.numeroDocumento").value(CARNE_EXTRANJERIA_INVALIDO));
        actualizar(id, "{\"nombres\":\"Ana\",\"tipoDocumento\":\"PASAPORTE\",\"numeroDocumento\":\"AB1234567\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.paisEmisorDocumento").value(PAIS_EMISOR_REQUERIDO));
        actualizar(id, "{\"nombres\":\"Ana\",\"tipoDocumento\":\"PASAPORTE\",\"numeroDocumento\":\"AB1\",\"paisEmisorDocumento\":\"CO\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.numeroDocumento").value(PASAPORTE_INVALIDO));
    }

    @Test
    void documentoDeOtroUsuarioDevuelve409ConCampo() throws Exception {
        UUID otro = usuario(registrar()), yo = usuario(registrar());
        actualizar(otro, "{\"nombres\":\"Otro\",\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"87654321\"}").andExpect(status().isOk());
        actualizar(yo, "{\"nombres\":\"Yo\",\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"87654321\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.campos.numeroDocumento").value(DOCUMENTO_DUPLICADO));
    }

    @Test
    void elRegistroUsaElMismoValidadorDeDocumento() throws Exception {
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombres\":\"Ana\",\"correoElectronico\":\"reg-" + UUID.randomUUID() + "@example.test\",\"password\":\"" + CLAVE
                        + "\",\"tipoDocumento\":\"PASAPORTE\",\"numeroDocumento\":\"12\",\"paisEmisorDocumento\":\"CO\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.numeroDocumento").value(PASAPORTE_INVALIDO));
    }

    // ---------- contraseña ----------

    @Test
    void cambiarContrasenaCierraLasOtrasSesionesYMantieneLaActual() throws Exception {
        String correo = registrar();
        Cookie actual = sesion(correo), otroDispositivo = sesion(correo);

        var respuesta = cambiar(actual, CLAVE, "nueva-clave-456").andExpect(status().isNoContent()).andReturn();
        Cookie rotada = respuesta.getResponse().getCookie("SESSION");
        assertNotNull(rotada, "La sesión actual se rota y sigue iniciada");
        assertNotEquals(actual.getValue(), rotada.getValue());

        mvc.perform(get("/api/perfil").cookie(rotada)).andExpect(status().isOk());
        mvc.perform(get("/api/perfil").cookie(otroDispositivo)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/perfil").cookie(actual)).andExpect(status().isUnauthorized());

        login(correo, CLAVE).andExpect(status().isUnauthorized());
        login(correo, "nueva-clave-456").andExpect(status().isNoContent());
    }

    @Test
    void contrasenaActualIncorrectaONuevaIgualSeRechazanConCampo() throws Exception {
        Cookie sesion = sesion(registrar());
        cambiar(sesion, "otra-clave-999", "nueva-clave-456")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.contrasenaActual").value(CONTRASENA_ACTUAL_INCORRECTA));
        cambiar(sesion, CLAVE, CLAVE)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.contrasenaNueva").value(CONTRASENA_NUEVA_IGUAL));
        cambiar(sesion, CLAVE, "corta").andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.contrasenaNueva").exists());
    }

    @Test
    void cuentaSoloConGoogleNoTieneContrasenaQueCambiar() throws Exception {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Google', ?)", id, id + "@example.test");
        jdbc.update("INSERT INTO usuario_identidad(id_usuario,proveedor,issuer,subject) VALUES (?, 'GOOGLE', 'https://accounts.google.com', ?)", id, "sub-" + id);
        mvc.perform(get("/api/perfil").with(authentication(como(id)))).andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneContrasena").value(false)).andExpect(jsonPath("$.tieneGoogle").value(true));
        mvc.perform(put("/api/perfil/contrasena").with(csrf()).with(authentication(como(id))).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contrasenaActual\":\"loquesea\",\"contrasenaNueva\":\"nueva-clave-456\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICTO"))
                .andExpect(jsonPath("$.campos.contrasenaActual").value(CUENTA_SIN_CONTRASENA));
    }
}
