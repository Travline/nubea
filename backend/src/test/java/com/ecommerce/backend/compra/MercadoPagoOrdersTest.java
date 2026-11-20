package com.ecommerce.backend.compra;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MercadoPagoOrdersTest {
    @Test
    void usaTokenDelVendedorYClaveEstableEnElServidor() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var auth = new AtomicReference<String>();
        var key = new AtomicReference<String>();
        var body = new AtomicReference<String>();
        var response = new AtomicReference<>("{\"id\":\"ORD123\",\"checkout_url\":\"https://www.mercadopago.com.pe/checkout/v1/redirect?abc=1\"}");
        server.createContext("/v1/orders", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            key.set(exchange.getRequestHeaders().getFirst("X-Idempotency-Key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] data = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, data.length);
            try (var out = exchange.getResponseBody()) { out.write(data); }
        });
        server.start();
        try {
            var cliente = new MercadoPagoOrders("http://127.0.0.1:" + server.getAddress().getPort());
            UUID intento = UUID.randomUUID(), pedido = UUID.randomUUID();
            URI retorno = URI.create("https://nubea.vercel.app/tienda/prueba/pedidos/" + pedido);
            var orden = cliente.crear("token-vendedor", intento, pedido, new BigDecimal("11.80"), retorno);
            assertEquals("ORD123", orden.id());
            assertEquals("Bearer token-vendedor", auth.get());
            assertEquals(intento.toString(), key.get());
            assertTrue(body.get().contains(pedido.toString()));
            assertTrue(body.get().contains("\"ticket\""));
            assertTrue(body.get().contains("\"11.80\""));
            assertFalse(body.get().contains("marketplace_fee"));
            assertFalse(body.get().contains("token-vendedor"));
            response.set("{\"id\":\"ORD123\",\"checkout_url\":\"https://evil.example/checkout\"}");
            assertThrows(MercadoPagoOrders.OrdenInciertaException.class,
                    () -> cliente.crear("token-vendedor", intento, pedido, new BigDecimal("11.80"), retorno));
        } finally { server.stop(0); }
    }
}
