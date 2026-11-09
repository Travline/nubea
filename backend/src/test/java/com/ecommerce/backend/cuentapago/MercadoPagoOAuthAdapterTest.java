package com.ecommerce.backend.cuentapago;

import com.ecommerce.backend.cuentapago.oauth.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MercadoPagoOAuthAdapterTest {
    @Test
    void enviaCanjeYRechazaRespuestasInvalidas() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> recibido = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger estado = new java.util.concurrent.atomic.AtomicInteger(200);
        AtomicReference<String> respuesta = new AtomicReference<>("{\"user_id\":123,\"access_token\":\"access\",\"refresh_token\":\"refresh\",\"expires_in\":3600}");
        server.createContext("/token", exchange -> {
            recibido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = respuesta.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(estado.get(), body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        server.start();
        try {
            var config = new OAuthProperties(true, "app", "secret", "https://auth.example/authorization",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/token", "https://backend.example/callback",
                    "https://panel.example/return", "k", Map.of(), 10, true);
            var adapter = new MercadoPagoOAuthAdapter(config);
            String url = adapter.urlAutorizacion("state", "challenge");
            assertTrue(url.contains("response_type=code"));
            assertTrue(url.contains("code_challenge_method=S256"));
            assertTrue(url.contains("redirect_uri=https%3A%2F%2Fbackend.example%2Fcallback"));
            var credentials = adapter.canjear("authorization-code", "verifier");
            assertEquals("123", credentials.idCuenta());
            assertEquals("access", credentials.accessToken());
            assertTrue(recibido.get().contains("\"grant_type\":\"authorization_code\""));
            assertTrue(recibido.get().contains("\"code_verifier\":\"verifier\""));
            respuesta.set("{\"user_id\":123,\"access_token\":\"access\"}");
            assertThrows(OAuthProveedorException.class, () -> adapter.canjear("code", "verifier"));
            respuesta.set("{\"error\":\"secret-provider-error\"}");
            estado.set(503);
            var fallo = assertThrows(OAuthProveedorException.class, () -> adapter.canjear("private-code", "private-verifier"));
            assertFalse(fallo.toString().contains("private"));
            assertFalse(fallo.toString().contains("secret-provider-error"));
            server.stop(0);
            assertThrows(OAuthProveedorException.class, () -> adapter.canjear("private-code", "private-verifier"));
        } finally { server.stop(0); }
    }
}
