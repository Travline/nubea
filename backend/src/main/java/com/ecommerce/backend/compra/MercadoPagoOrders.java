package com.ecommerce.backend.compra;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Cliente de transporte; el llamador debe guardar el intento antes de invocarlo. */
public final class MercadoPagoOrders {
    private final RestClient http;

    public MercadoPagoOrders() { this("https://api.mercadopago.com"); }

    MercadoPagoOrders(String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public record OrdenCreada(String id, URI checkoutUrl) {
        @Override public String toString() { return "OrdenCreada[id=" + id + ", checkoutUrl=[REDACTADA]]"; }
    }

    /** No reintentar con otra clave si no se conoce el resultado de esta solicitud. */
    public OrdenCreada crear(String token, UUID idempotencia, UUID pedido, BigDecimal total, URI retorno) {
        if (token == null || token.isBlank() || idempotencia == null || pedido == null || total == null
                || total.signum() <= 0 || total.scale() > 2 || retorno == null
                || !"https".equals(retorno.getScheme()) || retorno.getHost() == null
                || retorno.getUserInfo() != null || retorno.getFragment() != null)
            throw new IllegalArgumentException("Datos de la orden inválidos");
        Map<String, Object> body = Map.of(
                "type", "online", "processing_mode", "manual", "capture_mode", "automatic_async",
                "total_amount", total.setScale(2).toPlainString(), "external_reference", pedido.toString(),
                "expiration_time", "PT30M",
                "config", Map.of("online", Map.of("success_url", retorno.toString(), "failure_url", retorno.toString(),
                                "pending_url", retorno.toString(), "auto_return", "approved"),
                        "payment_method", Map.of("not_allowed_types", List.of("ticket"))));
        try {
            @SuppressWarnings("rawtypes")
            Map respuesta = http.post().uri("/v1/orders").headers(h -> { h.setBearerAuth(token); h.set("X-Idempotency-Key", idempotencia.toString()); })
                    .body(body).retrieve().body(Map.class);
            if (respuesta == null || !(respuesta.get("id") instanceof String id) || !id.matches("[A-Za-z0-9_-]{1,128}")
                    || !(respuesta.get("checkout_url") instanceof String enlace)) throw new IllegalStateException();
            URI url = URI.create(enlace);
            if (!"https".equals(url.getScheme()) || !"www.mercadopago.com.pe".equalsIgnoreCase(url.getHost())
                    || url.getUserInfo() != null || url.getPort() != -1) throw new IllegalStateException();
            return new OrdenCreada(id, url);
        } catch (RuntimeException ex) {
            // Una respuesta inválida o timeout no demuestra que la orden no se haya creado.
            throw new OrdenInciertaException();
        }
    }

    public static final class OrdenInciertaException extends RuntimeException {
        public OrdenInciertaException() { super("No se pudo confirmar la creación de la orden"); }
    }
}
