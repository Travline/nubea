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
@org.springframework.stereotype.Component
public class MercadoPagoOrders {
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

    public record Item(String nombre, int cantidad, BigDecimal precio) {}

    /** No reintentar con otra clave si no se conoce el resultado de esta solicitud. */
    public OrdenCreada crear(String token, UUID idempotencia, UUID pedido, BigDecimal total, List<Item> items, URI retorno) {
        return crear(token,idempotencia,pedido,total,items,retorno,List.of("ticket","bank_transfer","atm","digital_currency","account_money","prepaid_card"));
    }
    public OrdenCreada crear(String token, UUID idempotencia, UUID pedido, BigDecimal total, List<Item> items, URI retorno,List<String> excluidos) {
        if (token == null || token.isBlank() || idempotencia == null || pedido == null || total == null
                || total.signum() <= 0 || total.scale() > 2 || items == null || items.isEmpty() || retorno == null
                || !"https".equals(retorno.getScheme()) || retorno.getHost() == null
                || retorno.getUserInfo() != null || retorno.getFragment() != null)
            throw new IllegalArgumentException("Datos de la orden inválidos");
        if (items.stream().anyMatch(item -> item == null || item.nombre() == null || item.nombre().isBlank()
                || item.cantidad() < 1 || item.precio() == null || item.precio().signum() < 0 || item.precio().scale() > 2)
                || items.stream().map(item -> item.precio().multiply(BigDecimal.valueOf(item.cantidad())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(total) != 0)
            throw new IllegalArgumentException("Los artículos no coinciden con el total");
        var lineas = items.stream().map(item -> Map.of("title", item.nombre(), "quantity", item.cantidad(),
                "unit_price", item.precio().setScale(2).toPlainString())).toList();
        Map<String, Object> body = Map.of(
                "type", "online", "processing_mode", "manual", "capture_mode", "automatic_async",
                "total_amount", total.setScale(2).toPlainString(), "external_reference", pedido.toString(),
                "items", lineas,
                "expiration_time", "PT30M",
                "config", Map.of("online", Map.of("success_url", retorno.toString(), "failure_url", retorno.toString(),
                                "pending_url", retorno.toString(), "auto_return", "approved", "retries", Map.of("allowed", false)),
                        "payment_method", Map.of("not_allowed_types", excluidos)));
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
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized ex) { throw new TokenRechazado(); }
        catch (RuntimeException ex) {
            // Una respuesta inválida o timeout no demuestra que la orden no se haya creado.
            throw new OrdenInciertaException();
        }
    }

    /** Descubrir tipos del país del vendedor; solo crédito y débito se permiten. */
    public List<String> tiposExcluidos(String token) {
        try {
            List<?> metodos=http.get().uri("/v1/payment_methods").headers(h->h.setBearerAuth(token)).retrieve().body(List.class);
            if (metodos==null || metodos.isEmpty()) throw new OrdenInciertaException();
            var tipos=new java.util.TreeSet<String>();
            tipos.add("account_money"); tipos.add("digital_currency");
            for (Object metodo:metodos) {
                if (!(metodo instanceof Map<?,?> m) || !(m.get("payment_type_id") instanceof String tipo)
                        || !tipo.matches("[a-z_]{1,60}")) throw new OrdenInciertaException();
                if (!java.util.Set.of("credit_card","debit_card").contains(tipo)) tipos.add(tipo.equals("digital_wallet")?"digital_currency":tipo);
            }
            return List.copyOf(tipos);
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized ex) { throw new TokenRechazado(); }
        catch (RuntimeException ex) { throw new OrdenInciertaException(); }
    }
    public String verificarCuenta(String token, String esperada) {
        try {
            Map<?,?> cuenta=http.get().uri("/users/me").headers(h->h.setBearerAuth(token)).retrieve().body(Map.class);
            if (cuenta==null || !esperada.equals(String.valueOf(cuenta.get("id"))) || !"MPE".equals(cuenta.get("site_id"))
                    || !(cuenta.get("tags") instanceof List<?> tags)) throw new OrdenInciertaException();
            return tags.contains("test_user") ? "PRUEBA" : "PRODUCCION";
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized ex) { throw new TokenRechazado(); }
        catch (RuntimeException ex) { throw new OrdenInciertaException(); }
    }
    public void cancelar(String token, String id, UUID clave) { operar(token,id,clave,"cancel"); }
    public void reembolsar(String token, String id, UUID clave) { operar(token,id,clave,"refund"); }
    private void operar(String token,String id,UUID clave,String accion) {
        if (id==null || !id.matches("[A-Za-z0-9_-]{1,128}")) throw new IllegalArgumentException();
        try {
            http.post().uri("/v1/orders/{id}/"+accion,id)
                .headers(h->{ h.setBearerAuth(token); h.set("X-Idempotency-Key",clave.toString()); })
                .body(Map.of()).retrieve().toBodilessEntity();
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized ex) { throw new TokenRechazado(); }
        catch (RuntimeException ex) { throw new OrdenInciertaException(); }
    }
    public static final class TokenRechazado extends RuntimeException {}

    public record OrdenConsultada(String id, String referencia, String vendedor, String moneda,
                                  BigDecimal total, BigDecimal totalPagado, String estado, String detalle) {}

    /** Solo la respuesta de una consulta autenticada puede usarse para confirmar un pago. */
    public OrdenConsultada consultar(String token, String id) {
        if (token == null || token.isBlank() || id == null || !id.matches("[A-Za-z0-9_-]{1,128}"))
            throw new IllegalArgumentException("Datos de consulta inválidos");
        try {
            @SuppressWarnings("rawtypes")
            Map datos = http.get().uri("/v1/orders/{id}", id).headers(h -> h.setBearerAuth(token)).retrieve().body(Map.class);
            if (datos == null || !id.equals(datos.get("id"))) throw new IllegalStateException();
            String referencia = texto(datos, "external_reference"), vendedor = texto(datos, "user_id");
            String moneda = texto(datos, "currency"), estado = texto(datos, "status"), detalle = texto(datos, "status_detail");
            BigDecimal total = importe(datos, "total_amount"), pagado = importe(datos, "total_paid_amount");
            return new OrdenConsultada(id, referencia, vendedor, moneda, total, pagado, estado, detalle);
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized ex) { throw new TokenRechazado(); }
        catch (RuntimeException ex) { throw new OrdenInciertaException(); }
    }
    @SuppressWarnings("rawtypes")
    private static String texto(Map datos, String campo) {
        Object valor = datos.get(campo);
        if (!(valor instanceof String) && !(valor instanceof Number)) throw new IllegalStateException();
        String texto = valor.toString();
        if (texto.isBlank() || texto.length() > 128) throw new IllegalStateException();
        return texto;
    }
    @SuppressWarnings("rawtypes")
    private static BigDecimal importe(Map datos, String campo) {
        BigDecimal importe = new BigDecimal(texto(datos, campo));
        if (importe.signum() < 0 || importe.scale() > 2) throw new IllegalStateException();
        return importe;
    }

    public static final class OrdenInciertaException extends RuntimeException {
        public OrdenInciertaException() { super("No se pudo confirmar la creación de la orden"); }
    }
}
