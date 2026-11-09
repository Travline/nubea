package com.ecommerce.backend.cuentapago;

import static org.junit.jupiter.api.Assertions.*;
import com.ecommerce.backend.cuentapago.oauth.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "OAUTH_PG_TEST", matches = "true")
class OAuthPostgresTest {
    private static final String SCHEMA = "oauth_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String URL = System.getenv("SPRING_DATASOURCE_URL");
    private static com.sun.net.httpserver.HttpServer servidor;
    private static final java.util.concurrent.atomic.AtomicReference<String> RESPUESTA = new java.util.concurrent.atomic.AtomicReference<>();
    @Autowired OAuthTransactionStore store;
    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) throws Exception {
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA " + SCHEMA);
            s.execute("CREATE TABLE " + SCHEMA + ".tienda (id_tienda uuid PRIMARY KEY, id_propietario uuid NOT NULL, estado text NOT NULL, nombre text DEFAULT 'Tienda', slug text DEFAULT 'tienda')");
            s.execute("CREATE TABLE " + SCHEMA + ".cuenta_pago_tienda (id_tienda uuid PRIMARY KEY, proveedor text DEFAULT 'MERCADO_PAGO', estado text DEFAULT 'PENDIENTE', id_cuenta_externa text UNIQUE, referencia_credencial text, fecha_vinculacion timestamptz, fecha_actualizacion timestamptz DEFAULT now())");
            s.execute("CREATE TABLE " + SCHEMA + ".oauth_mercado_pago_intento (id_intento uuid PRIMARY KEY, id_tienda uuid NOT NULL, id_usuario uuid NOT NULL, state_hash bytea UNIQUE NOT NULL, verifier_cifrado text NOT NULL, retorno_panel text NOT NULL, fecha_creacion timestamptz NOT NULL, fecha_expiracion timestamptz NOT NULL, estado text NOT NULL)");
            s.execute("CREATE UNIQUE INDEX ON " + SCHEMA + ".oauth_mercado_pago_intento(id_tienda) WHERE estado='PENDIENTE'");
            s.execute("CREATE TABLE " + SCHEMA + ".cuenta_pago_historial(id_historial bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, id_tienda uuid, id_usuario uuid, estado_anterior text, estado_nuevo text, fecha_cambio timestamptz)");
        }
        servidor = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/token", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = RESPUESTA.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        servidor.start();
        registry.add("mercadopago.oauth.allow-insecure-localhost", () -> "true");
        registry.add("spring.datasource.url", () -> URL + (URL.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registry.add("mercadopago.oauth.enabled", () -> "true");
        registry.add("mercadopago.oauth.client-id", () -> "client");
        registry.add("mercadopago.oauth.client-secret", () -> "secret");
        registry.add("mercadopago.oauth.authorization-url", () -> "https://auth.example/authorization");
        registry.add("mercadopago.oauth.token-url", () -> "http://127.0.0.1:" + servidor.getAddress().getPort() + "/token");
        registry.add("mercadopago.oauth.callback-url", () -> "https://api.example/callback");
        registry.add("mercadopago.oauth.retorno-panel", () -> "https://panel.example/resultado");
        registry.add("mercadopago.oauth.active-key", () -> "k1");
        registry.add("mercadopago.oauth.keys.k1", () -> Base64.getEncoder().encodeToString(new byte[32]));
        registry.add("mercadopago.oauth.expiry-minutes", () -> "10");
    }
    static Connection connection() throws Exception { return DriverManager.getConnection(URL, System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("SPRING_DATASOURCE_PASSWORD")); }
    @AfterAll static void cleanup() throws Exception { if (servidor != null) servidor.stop(0); try (Connection c = connection(); Statement s = c.createStatement()) { s.execute("DROP SCHEMA " + SCHEMA + " CASCADE"); } }

    @Test
    void consumoUnicoYActivacionNoPermiteIntentoAnterior() throws Exception {
        UUID tienda = UUID.randomUUID(), usuario = UUID.randomUUID();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("INSERT INTO " + SCHEMA + ".tienda(id_tienda,id_propietario,estado) VALUES (?,?,'ACTIVA')")) { p.setObject(1, tienda); p.setObject(2, usuario); p.executeUpdate(); }
        byte[] a = new byte[32]; a[0] = 1;
        byte[] b = new byte[32]; b[0] = 2;
        store.iniciar(UUID.randomUUID(), tienda, usuario, a, "cifrado", "https://panel.example/resultado", 10);
        var intento = store.consumir(a);
        assertThrows(OAuthEstadoInvalidoException.class, () -> store.consumir(a));
        store.iniciar(UUID.randomUUID(), tienda, usuario, b, "cifrado", "https://panel.example/resultado", 10);
        assertThrows(OAuthConflictoException.class, () -> store.activar(intento, "123", "cifrado"));
        var segundo = store.consumir(b);
        store.activar(segundo, "123", "cifrado");
        assertThrows(OAuthConflictoException.class, () -> store.iniciar(UUID.randomUUID(), tienda, usuario, a, "cifrado", "https://panel.example/resultado", 10));
    }
    @Test
    void iniciosSimultaneosMantienenUnSoloIntentoPendiente() throws Exception {
        UUID tienda = UUID.randomUUID(), usuario = UUID.randomUUID();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("INSERT INTO " + SCHEMA + ".tienda(id_tienda,id_propietario,estado) VALUES (?,?,'ACTIVA')")) {
            p.setObject(1, tienda); p.setObject(2, usuario); p.executeUpdate();
        }
        var start = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> { start.await(); byte[] hash = new byte[32]; hash[0] = 3; store.iniciar(UUID.randomUUID(), tienda, usuario, hash, "cifrado", "https://panel.example", 10); return null; });
            var b = executor.submit(() -> { start.await(); byte[] hash = new byte[32]; hash[0] = 4; store.iniciar(UUID.randomUUID(), tienda, usuario, hash, "cifrado", "https://panel.example", 10); return null; });
            start.countDown();
            a.get(15, java.util.concurrent.TimeUnit.SECONDS); b.get(15, java.util.concurrent.TimeUnit.SECONDS);
            try (Connection c = connection(); PreparedStatement p = c.prepareStatement("SELECT count(*) FROM " + SCHEMA + ".oauth_mercado_pago_intento WHERE id_tienda=? AND estado='PENDIENTE'")) {
                p.setObject(1, tienda); try (ResultSet rs = p.executeQuery()) { assertTrue(rs.next()); assertEquals(1, rs.getInt(1)); }
            }
        } finally { executor.shutdownNow(); }
    }

    @Test
    void endpointValidaPropietarioYConsumeCancelacion() throws Exception {
        UUID tienda = UUID.randomUUID(), propietario = UUID.randomUUID(), otro = UUID.randomUUID();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("INSERT INTO " + SCHEMA + ".tienda(id_tienda,id_propietario,estado) VALUES (?,?,'BORRADOR')")) {
            p.setObject(1, tienda); p.setObject(2, propietario); p.executeUpdate();
        }
        String ruta = "/api/tiendas/" + tienda + "/cuenta-pago/mercado-pago/oauth/inicios";
        var ajeno = UsernamePasswordAuthenticationToken.authenticated(otro, null, List.of());
        var dueno = UsernamePasswordAuthenticationToken.authenticated(propietario, null, List.of());
        mvc.perform(post(ruta)).andExpect(status().is4xxClientError());
        mvc.perform(post(ruta).with(authentication(ajeno))).andExpect(status().isForbidden());
        var result = mvc.perform(post(ruta).with(authentication(dueno))).andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store"))).andReturn();
        String json = result.getResponse().getContentAsString();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("state=([A-Za-z0-9_-]{43})").matcher(json);
        assertTrue(matcher.find());
        String state = matcher.group(1);
        String callback = "/api/integraciones/mercado-pago/oauth/callback";
        mvc.perform(get(callback).param("state", state).param("error", "access_denied"))
                .andExpect(status().isSeeOther()).andExpect(header().string("Location", "https://panel.example/resultado?resultado=error"));
        mvc.perform(get(callback).param("state", state).param("code", "x")).andExpect(status().isBadRequest());
    }

    @Test
    void callbackExitosoPersisteCredencialesCifradasYConflictoNoActiva() throws Exception {
        UUID propietario = UUID.randomUUID(), tienda = UUID.randomUUID(), otra = UUID.randomUUID();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("INSERT INTO " + SCHEMA + ".tienda(id_tienda,id_propietario,estado) VALUES (?,?,'ACTIVA')")) {
            for (UUID id : List.of(tienda, otra)) { p.setObject(1, id); p.setObject(2, propietario); p.executeUpdate(); }
        }
        RESPUESTA.set("{\"user_id\":12345,\"access_token\":\"access-secret\",\"refresh_token\":\"refresh-secret\",\"expires_in\":3600}");
        var auth = UsernamePasswordAuthenticationToken.authenticated(propietario, null, List.of());
        String callback = "/api/integraciones/mercado-pago/oauth/callback";
        for (UUID id : List.of(tienda, otra)) {
            String ruta = "/api/tiendas/" + id + "/cuenta-pago/mercado-pago/oauth/inicios";
            String json = mvc.perform(post(ruta).with(authentication(auth))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            var matcher = java.util.regex.Pattern.compile("state=([A-Za-z0-9_-]{43})").matcher(json);
            assertTrue(matcher.find());
            mvc.perform(get(callback).param("state", matcher.group(1)).param("code", "codigo"))
                    .andExpect(status().isSeeOther()).andExpect(header().string("Location", "https://panel.example/resultado?resultado=" + (id.equals(tienda) ? "exito" : "conflicto")));
        }
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("SELECT estado, id_cuenta_externa, referencia_credencial, fecha_vinculacion FROM " + SCHEMA + ".cuenta_pago_tienda WHERE id_tienda=?")) {
            p.setObject(1, tienda);
            try (ResultSet rs = p.executeQuery()) { assertTrue(rs.next()); assertEquals("ACTIVA", rs.getString(1)); assertEquals("12345", rs.getString(2)); assertFalse(rs.getString(3).contains("secret")); assertNotNull(rs.getTimestamp(4)); }
            p.setObject(1, otra);
            try (ResultSet rs = p.executeQuery()) { assertTrue(rs.next()); assertEquals("PENDIENTE", rs.getString(1)); assertNull(rs.getString(2)); assertNull(rs.getString(3)); assertNull(rs.getTimestamp(4)); }
        }
    }

    @Test
    void vencimientoYPropiedadImpidenActivacion() throws Exception {
        UUID tienda = UUID.randomUUID(), usuario = UUID.randomUUID(), nuevo = UUID.randomUUID();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("INSERT INTO " + SCHEMA + ".tienda(id_tienda,id_propietario,estado) VALUES (?,?,'ACTIVA')")) {
            p.setObject(1, tienda); p.setObject(2, usuario); p.executeUpdate();
        }
        byte[] vencido = new byte[32]; vencido[0] = 40;
        UUID id = UUID.randomUUID();
        store.iniciar(id, tienda, usuario, vencido, "cifrado", "https://panel.example", 10);
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("UPDATE " + SCHEMA + ".oauth_mercado_pago_intento SET fecha_creacion=now()-interval '2 minutes', fecha_expiracion=now()-interval '1 minute' WHERE id_intento=?")) {
            p.setObject(1, id); p.executeUpdate();
        }
        assertThrows(OAuthEstadoInvalidoException.class, () -> store.consumir(vencido));
        byte[] actual = new byte[32]; actual[0] = 41;
        store.iniciar(UUID.randomUUID(), tienda, usuario, actual, "cifrado", "https://panel.example", 10);
        var intento = store.consumir(actual);
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("UPDATE " + SCHEMA + ".tienda SET id_propietario=? WHERE id_tienda=?")) {
            p.setObject(1, nuevo); p.setObject(2, tienda); p.executeUpdate();
        }
        assertThrows(OAuthConflictoException.class, () -> store.validar(intento));
        assertThrows(OAuthConflictoException.class, () -> store.activar(intento, "999", "cifrado"));
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("SELECT estado, referencia_credencial FROM " + SCHEMA + ".cuenta_pago_tienda WHERE id_tienda=?")) {
            p.setObject(1, tienda); try (ResultSet rs = p.executeQuery()) { assertTrue(rs.next()); assertEquals("PENDIENTE", rs.getString(1)); assertNull(rs.getString(2)); }
        }
    }

    @Test
    void errorProveedorConsumeIntentoSinActivar() throws Exception {
        UUID propietario = UUID.randomUUID(), tienda = UUID.randomUUID();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("INSERT INTO " + SCHEMA + ".tienda(id_tienda,id_propietario,estado) VALUES (?,?,'ACTIVA')")) {
            p.setObject(1, tienda); p.setObject(2, propietario); p.executeUpdate();
        }
        RESPUESTA.set("{\"error\":\"invalid_grant\"}");
        var auth = UsernamePasswordAuthenticationToken.authenticated(propietario, null, List.of());
        String ruta = "/api/tiendas/" + tienda + "/cuenta-pago/mercado-pago/oauth/inicios";
        String json = mvc.perform(post(ruta).with(authentication(auth))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var matcher = java.util.regex.Pattern.compile("state=([A-Za-z0-9_-]{43})").matcher(json); assertTrue(matcher.find());
        String state = matcher.group(1), callback = "/api/integraciones/mercado-pago/oauth/callback";
        mvc.perform(get(callback).param("state", state).param("code", "codigo"))
                .andExpect(status().isSeeOther()).andExpect(header().string("Location", "https://panel.example/resultado?resultado=error"));
        mvc.perform(get(callback).param("state", state).param("code", "codigo")).andExpect(status().isBadRequest());
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("SELECT estado, referencia_credencial FROM " + SCHEMA + ".cuenta_pago_tienda WHERE id_tienda=?")) {
            p.setObject(1, tienda); try (ResultSet rs = p.executeQuery()) { assertTrue(rs.next()); assertEquals("PENDIENTE", rs.getString(1)); assertNull(rs.getString(2)); }
        }
    }

    @Test
    void suspensionDespuesDeConsumirImpideActivacion() throws Exception {
        UUID tienda = UUID.randomUUID(), usuario = UUID.randomUUID();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("INSERT INTO " + SCHEMA + ".tienda(id_tienda,id_propietario,estado) VALUES (?,?,'ACTIVA')")) {
            p.setObject(1, tienda); p.setObject(2, usuario); p.executeUpdate();
        }
        byte[] hash = new byte[32]; hash[0] = 42;
        store.iniciar(UUID.randomUUID(), tienda, usuario, hash, "cifrado", "https://panel.example", 10);
        var intento = store.consumir(hash);
        store.validar(intento);
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("UPDATE " + SCHEMA + ".tienda SET estado='SUSPENDIDA' WHERE id_tienda=?")) {
            p.setObject(1, tienda); p.executeUpdate();
        }
        assertThrows(OAuthConflictoException.class, () -> store.activar(intento, "998", "cifrado"));
        assertThrows(OAuthConflictoException.class, () -> store.iniciar(UUID.randomUUID(), tienda, usuario, new byte[32], "cifrado", "https://panel.example", 10));
    }

    @Test
    void desconexionInvalidaCallbackYEsIdempotente() throws Exception {
        UUID tienda = UUID.randomUUID(), usuario = UUID.randomUUID();
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("INSERT INTO " + SCHEMA + ".tienda(id_tienda,id_propietario,estado) VALUES (?,?,'ACTIVA')")) {
            p.setObject(1, tienda); p.setObject(2, usuario); p.executeUpdate();
        }
        byte[] hash = new byte[32]; hash[0] = 81;
        store.iniciar(UUID.randomUUID(), tienda, usuario, hash, "cifrado", "https://panel.example", 10);
        var intento = store.consumir(hash);
        var auth = UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of());
        String ruta = "/api/tiendas/" + tienda + "/cuenta-pago/desconexion";
        mvc.perform(post(ruta).with(authentication(auth))).andExpect(status().isNoContent());
        mvc.perform(post(ruta).with(authentication(auth))).andExpect(status().isNoContent());
        assertThrows(OAuthConflictoException.class, () -> store.activar(intento, "998", "cifrado"));
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement("SELECT estado, referencia_credencial, id_cuenta_externa, (SELECT count(*) FROM " + SCHEMA + ".cuenta_pago_historial h WHERE h.id_tienda=cp.id_tienda) FROM " + SCHEMA + ".cuenta_pago_tienda cp WHERE id_tienda=?")) {
            p.setObject(1, tienda); try (ResultSet rs = p.executeQuery()) { assertTrue(rs.next()); assertEquals("DESCONECTADA", rs.getString(1)); assertNull(rs.getString(2)); assertNull(rs.getString(3)); assertEquals(1, rs.getInt(4)); }
        }
    }

}
