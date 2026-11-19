package com.ecommerce.backend.cuentapago.oauth;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(prefix = "mercadopago.oauth", name = "enabled", havingValue = "true")
public class MercadoPagoOAuthAdapter implements MercadoPagoOAuthPort {
    private final OAuthProperties config;
    private final RestClient client;
    public MercadoPagoOAuthAdapter(OAuthProperties config) {
        this.config = config;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder().requestFactory(factory).build();
    }
    public String urlAutorizacion(String state, String challenge) {
        return UriComponentsBuilder.fromUriString(config.authorizationUrl()).queryParam("client_id", config.clientId())
                .queryParam("response_type", "code").queryParam("platform_id", "mp").queryParam("redirect_uri", "{callback}")
                .queryParam("state", state).queryParam("code_challenge", challenge).queryParam("code_challenge_method", "S256")
                .encode().buildAndExpand(config.callbackUrl()).toUriString();
    }
    public Credenciales canjear(String code, String verifier) {
        try {
            @SuppressWarnings("rawtypes")
            Map response = client.post().uri(config.tokenUrl()).body(Map.of("client_id", config.clientId(), "client_secret", config.clientSecret(),
                    "grant_type", "authorization_code", "code", code, "redirect_uri", config.callbackUrl(), "code_verifier", verifier))
                    .retrieve().body(Map.class);
            if (response == null || response.get("user_id") == null || !(response.get("access_token") instanceof String access) || access.isBlank()
                    || !(response.get("refresh_token") instanceof String refresh) || refresh.isBlank()
                    || !response.get("user_id").toString().matches("[0-9]{1,100}")) throw new IllegalStateException();
            Object expires = response.get("expires_in");
            return new Credenciales(response.get("user_id").toString(), access, refresh, expires instanceof Number n ? n.longValue() : null);
        } catch (RuntimeException ex) { throw new OAuthProveedorException(); }
    }
}
