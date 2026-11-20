package com.ecommerce.backend.compra;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.List;
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
            var orden = cliente.crear("token-vendedor", intento, pedido, new BigDecimal("11.80"), List.of(new MercadoPagoOrders.Item("Producto", 2, new BigDecimal("5.90"))), retorno);
            assertEquals("ORD123", orden.id());
            assertEquals("Bearer token-vendedor", auth.get());
            assertEquals(intento.toString(), key.get());
            assertTrue(body.get().contains(pedido.toString()));
            assertTrue(body.get().contains("\"ticket\""));
            assertTrue(body.get().contains("\"11.80\""));
            assertTrue(body.get().contains("\"quantity\":2"));
            assertTrue(body.get().contains("\"unit_price\":\"5.90\""));
            assertThrows(IllegalArgumentException.class, () -> cliente.crear("token-vendedor", intento, pedido,
                    new BigDecimal("11.81"), List.of(new MercadoPagoOrders.Item("Producto", 2, new BigDecimal("5.90"))), retorno));
            assertFalse(body.get().contains("marketplace_fee"));
            assertFalse(body.get().contains("token-vendedor"));
            response.set("{\"id\":\"ORD123\",\"checkout_url\":\"https://evil.example/checkout\"}");
            assertThrows(MercadoPagoOrders.OrdenInciertaException.class,
                    () -> cliente.crear("token-vendedor", intento, pedido, new BigDecimal("11.80"), List.of(new MercadoPagoOrders.Item("Producto", 2, new BigDecimal("5.90"))), retorno));
        } finally { server.stop(0); }
    }
    @Test
    void consultaOrdenAutenticadaYRechazaRespuestaInconsistente() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var auth = new AtomicReference<String>();
        var response = new AtomicReference<>("{\"id\":\"ORD123\",\"external_reference\":\"ref\",\"user_id\":12345,\"currency\":\"PEN\",\"total_amount\":\"11.80\",\"total_paid_amount\":\"11.80\",\"status\":\"processed\",\"status_detail\":\"accredited\"}");
        server.createContext("/v1/orders/ORD123", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] data = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, data.length);
            try (var out = exchange.getResponseBody()) { out.write(data); }
        });
        server.start();
        try {
            var cliente = new MercadoPagoOrders("http://127.0.0.1:" + server.getAddress().getPort());
            var orden = cliente.consultar("token-vendedor", "ORD123");
            assertEquals("Bearer token-vendedor", auth.get());
            assertEquals("12345", orden.vendedor());
            assertEquals(0, new BigDecimal("11.80").compareTo(orden.totalPagado()));
            response.set("{\"id\":\"OTRA\"}");
            assertThrows(MercadoPagoOrders.OrdenInciertaException.class, () -> cliente.consultar("token-vendedor", "ORD123"));
        } finally { server.stop(0); }
    }
    @Test
    void verificaAmbienteExcluyeMediosNoTarjetaYReembolsaConClaveEstable() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var path=new AtomicReference<String>(); var body=new AtomicReference<String>(); var key=new AtomicReference<String>();
        server.createContext("/",exchange->{
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            key.set(exchange.getRequestHeaders().getFirst("X-Idempotency-Key"));
            String response=switch(path.get()) {
                case "/users/me" -> "{\"id\":123,\"site_id\":\"MPE\",\"tags\":[\"test_user\"]}";
                case "/v1/payment_methods" -> "[{\"payment_type_id\":\"credit_card\"},{\"payment_type_id\":\"debit_card\"},{\"payment_type_id\":\"ticket\"},{\"payment_type_id\":\"bank_transfer\"}]";
                default -> "{}";
            };
            byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(200,bytes.length);
            try(var out=exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        try {
            var cliente=new MercadoPagoOrders("http://127.0.0.1:"+server.getAddress().getPort());
            assertEquals("PRUEBA",cliente.verificarCuenta("token","123"));
            assertThrows(MercadoPagoOrders.OrdenInciertaException.class,()->cliente.verificarCuenta("token","otro"));
            var excluidos=cliente.tiposExcluidos("token");
            assertTrue(excluidos.containsAll(List.of("ticket","bank_transfer","account_money")));
            assertFalse(excluidos.contains("credit_card")); assertFalse(excluidos.contains("debit_card"));
            UUID clave=UUID.randomUUID(); cliente.reembolsar("token","ORD123",clave);
            assertEquals("/v1/orders/ORD123/refund",path.get()); assertEquals("{}",body.get()); assertEquals(clave.toString(),key.get());
            cliente.cancelar("token","ORD123",clave); assertEquals("/v1/orders/ORD123/cancel",path.get());
        } finally { server.stop(0); }
    }
}
