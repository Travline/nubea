package com.ecommerce.backend;

import static com.ecommerce.backend.shared.exception.MensajesUsuario.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bloque 5: inicio de sesión con Google (RN-M1-05/06). No llama a Google: un servidor HTTP local sirve el JWKS
 * de una clave RSA propia y el código de producción lo descarga igual que el de Google.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GoogleLoginIntegracionTest {
    static final String CLIENT_ID = "nubea-test.apps.googleusercontent.com";
    static final RSAKey CLAVE_GOOGLE, CLAVE_AJENA;
    static final HttpServer JWKS;

    static {
        try {
            CLAVE_GOOGLE = new RSAKeyGenerator(2048).keyID("google-test").generate();
            CLAVE_AJENA = new RSAKeyGenerator(2048).keyID("google-test").generate(); // mismo kid, otra clave
            byte[] jwks = new JWKSet(CLAVE_GOOGLE.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            JWKS = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            JWKS.createContext("/certs", ex -> {
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(200, jwks.length);
                ex.getResponseBody().write(jwks);
                ex.close();
            });
            JWKS.start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void google(DynamicPropertyRegistry registry) {
        registry.add("google.client-id", () -> CLIENT_ID);
        registry.add("google.jwk-set-uri", () -> "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/certs");
    }

    @AfterAll
    static void detener() {
        JWKS.stop(0);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    // ---------- tokens ----------

    private static JWTClaimsSet.Builder claims(String sub, String email) {
        Instant ahora = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com").audience(CLIENT_ID).subject(sub)
                .issueTime(Date.from(ahora)).expirationTime(Date.from(ahora.plusSeconds(3600)))
                .claim("email", email).claim("email_verified", true)
                .claim("given_name", "Ana").claim("family_name", "Pérez García");
    }

    private static String firmar(RSAKey clave, JWTClaimsSet claims) throws Exception {
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(clave.getKeyID()).type(JOSEObjectType.JWT).build(), claims);
        jwt.sign(new RSASSASigner(clave));
        return jwt.serialize();
    }

    private static String token(String sub, String email, Consumer<JWTClaimsSet.Builder> ajuste) throws Exception {
        var b = claims(sub, email);
        ajuste.accept(b);
        return firmar(CLAVE_GOOGLE, b.build());
    }

    private static String token(String sub, String email) throws Exception {
        return token(sub, email, b -> {});
    }

    private ResultActions loginGoogle(String idToken) throws Exception {
        return mvc.perform(post("/api/auth/google").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"" + idToken + "\"}"));
    }

    private static String nuevoSub() {
        return "sub-" + UUID.randomUUID();
    }

    private static String nuevoCorreo() {
        return "g-" + UUID.randomUUID() + "@example.test";
    }

    private UUID usuarioLocal(String email, String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email,estado" + ("CERRADO".equals(estado) ? ",fecha_cierre" : "")
                + ") VALUES (?, 'Local', ?, ?" + ("CERRADO".equals(estado) ? ",now()" : "") + ")", id, email, estado);
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='CLIENTE'", id);
        return id;
    }

    private void identidadGoogle(UUID usuario, String sub) {
        jdbc.update("INSERT INTO usuario_identidad(id_usuario,proveedor,issuer,subject) VALUES (?, 'GOOGLE', 'https://accounts.google.com', ?)", usuario, sub);
    }

    // ---------- inicio de sesión ----------

    @Test
    void tokenValidoCreaLaCuentaEIniciaSesionYLuegoSoloIniciaSesion() throws Exception {
        String sub = nuevoSub(), correo = nuevoCorreo();
        var creado = loginGoogle(token(sub, correo.toUpperCase())).andExpect(status().isCreated())
                .andExpect(jsonPath("$.correoElectronico").value(correo))
                .andExpect(jsonPath("$.nombres").value("Ana"))
                .andExpect(jsonPath("$.apellidoPaterno").value("Pérez García"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.roles[0]").value("CLIENTE"))
                .andReturn();
        var sesion = creado.getResponse().getCookie("SESSION");
        assertNotNull(sesion, "Debe iniciar sesión igual que el login local");
        mvc.perform(get("/api/auth/me").cookie(sesion)).andExpect(status().isOk()).andExpect(jsonPath("$.correoElectronico").value(correo));

        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM usuario_identidad i JOIN usuario_plataforma u USING (id_usuario)
                WHERE u.email=? AND i.proveedor='GOOGLE' AND i.issuer='https://accounts.google.com' AND i.subject=? AND i.password_hash IS NULL
                """, Integer.class, correo, sub));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuario_identidad i JOIN usuario_plataforma u USING (id_usuario) WHERE u.email=? AND i.proveedor='LOCAL'", Integer.class, correo));

        // Segunda vez (aunque Google cambie el formato del emisor): solo inicia sesión, sin duplicar la identidad.
        loginGoogle(token(sub, correo, b -> b.issuer("accounts.google.com"))).andExpect(status().isNoContent());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM usuario_identidad WHERE subject=?", Integer.class, sub));
    }

    @Test
    void sinNombreEnElTokenUsaLaParteLocalDelCorreo() throws Exception {
        String correo = nuevoCorreo();
        loginGoogle(token(nuevoSub(), correo, b -> b.claim("given_name", null).claim("family_name", null).claim("name", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombres").value(correo.substring(0, correo.indexOf('@'))));
    }

    @Test
    void correoYaRegistradoConContrasenaDevuelve409SinVincular() throws Exception {
        String correo = nuevoCorreo(), sub = nuevoSub();
        usuarioLocal(correo, "ACTIVO");
        loginGoogle(token(sub, correo)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICADO"))
                .andExpect(jsonPath("$.mensaje").value(GOOGLE_CORREO_YA_REGISTRADO))
                .andExpect(jsonPath("$.campos.correoElectronico").value(GOOGLE_CORREO_YA_REGISTRADO));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuario_identidad WHERE subject=?", Integer.class, sub));
    }

    @Test
    void usuarioSuspendidoOCerradoSeRechazaComoElLoginLocal() throws Exception {
        for (String estado : List.of("SUSPENDIDO", "CERRADO")) {
            String sub = nuevoSub();
            identidadGoogle(usuarioLocal(nuevoCorreo(), estado), sub);
            loginGoogle(token(sub, nuevoCorreo())).andExpect(status().isUnauthorized())
                    .andExpect(cookie().doesNotExist("SESSION"));
        }
    }

    @Test
    void correoDeGoogleNoVerificadoDevuelve422() throws Exception {
        loginGoogle(token(nuevoSub(), nuevoCorreo(), b -> b.claim("email_verified", false)))
                .andExpect(status().is(422)).andExpect(jsonPath("$.campos.idToken").value(GOOGLE_CORREO_NO_VERIFICADO));
        loginGoogle(token(nuevoSub(), nuevoCorreo(), b -> b.claim("email_verified", null)))
                .andExpect(status().is(422));
    }

    // ---------- tokens rechazados ----------

    private void rechazado(String idToken) throws Exception {
        loginGoogle(idToken).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("GOOGLE_TOKEN_INVALIDO"))
                .andExpect(jsonPath("$.mensaje").value(GOOGLE_TOKEN_INVALIDO));
    }

    @Test
    void firmaInvalidaAudienciaEmisorYExpiracionSeRechazan() throws Exception {
        rechazado(firmar(CLAVE_AJENA, claims(nuevoSub(), nuevoCorreo()).build()));                 // firma de otra clave
        rechazado(token(nuevoSub(), nuevoCorreo(), b -> b.audience("otra-app.apps.googleusercontent.com")));
        rechazado(token(nuevoSub(), nuevoCorreo(), b -> b.issuer("https://evil.example.com")));
        rechazado(token(nuevoSub(), nuevoCorreo(), b -> b.issueTime(Date.from(Instant.now().minusSeconds(7200)))
                .expirationTime(Date.from(Instant.now().minusSeconds(3600)))));                      // expirado
        rechazado(token(nuevoSub(), nuevoCorreo(), b -> b.issueTime(null)));                          // sin iat
        rechazado("esto-no-es-un-jwt");
        String valido = token(nuevoSub(), nuevoCorreo());
        rechazado(valido.substring(0, valido.lastIndexOf('.') + 1) + "AAAA");                         // firma alterada
    }

    @Test
    void seRechazanAlgNoneYHs256FirmadoConUnSecreto() throws Exception {
        // alg=none: token sin firma
        rechazado(new PlainJWT(claims(nuevoSub(), nuevoCorreo()).build()).serialize());
        // HS256 con un secreto (p. ej. usando la clave pública como secreto HMAC): solo se admite RS256
        var hs = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).keyID("google-test").build(), claims(nuevoSub(), nuevoCorreo()).build());
        hs.sign(new MACSigner("un-secreto-de-prueba-de-al-menos-256-bits!!".getBytes(StandardCharsets.UTF_8)));
        rechazado(hs.serialize());
    }

    @Test
    void exigeCsrfComoElLoginLocal() throws Exception {
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"" + token(nuevoSub(), nuevoCorreo()) + "\"}")).andExpect(status().isForbidden());
    }

    // ---------- vinculación ----------

    private ResultActions vincular(UUID usuario, String idToken) throws Exception {
        var req = post("/api/auth/google/vinculacion").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"" + idToken + "\"}");
        if (usuario != null) req = req.with(authentication(UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of())));
        return mvc.perform(req);
    }

    @Test
    void vinculacionCorrectaYLuegoInicioDeSesionConGoogle() throws Exception {
        String correoLocal = nuevoCorreo(), sub = nuevoSub();
        UUID usuario = usuarioLocal(correoLocal, "ACTIVO");
        vincular(usuario, token(sub, nuevoCorreo())).andExpect(status().isNoContent());   // el correo de Google puede ser otro
        vincular(usuario, token(sub, nuevoCorreo())).andExpect(status().isNoContent());   // repetir no cambia nada
        assertEquals(usuario, jdbc.queryForObject("SELECT id_usuario FROM usuario_identidad WHERE subject=?", UUID.class, sub));
        var login = loginGoogle(token(sub, nuevoCorreo())).andExpect(status().isNoContent()).andReturn();
        mvc.perform(get("/api/auth/me").cookie(login.getResponse().getCookie("SESSION")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correoElectronico").value(correoLocal));
    }

    @Test
    void subjectDeOtroUsuarioYSegundaCuentaDeGoogleDevuelven409() throws Exception {
        String sub = nuevoSub();
        identidadGoogle(usuarioLocal(nuevoCorreo(), "ACTIVO"), sub);
        UUID yo = usuarioLocal(nuevoCorreo(), "ACTIVO");
        vincular(yo, token(sub, nuevoCorreo())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.campos.idToken").value(GOOGLE_DE_OTRO_USUARIO));

        identidadGoogle(yo, nuevoSub());
        vincular(yo, token(nuevoSub(), nuevoCorreo())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.campos.idToken").value(GOOGLE_YA_VINCULADO_OTRA));
    }

    @Test
    void vincularExigeSesionYTokenValido() throws Exception {
        vincular(null, token(nuevoSub(), nuevoCorreo())).andExpect(status().isUnauthorized());
        vincular(usuarioLocal(nuevoCorreo(), "ACTIVO"), firmar(CLAVE_AJENA, claims(nuevoSub(), nuevoCorreo()).build()))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("GOOGLE_TOKEN_INVALIDO"));
    }
}
