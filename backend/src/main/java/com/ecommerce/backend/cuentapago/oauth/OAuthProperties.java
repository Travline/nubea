package com.ecommerce.backend.cuentapago.oauth;

import java.net.URI;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mercadopago.oauth")
public record OAuthProperties(boolean enabled, String clientId, String clientSecret, String authorizationUrl,
        String tokenUrl, String callbackUrl, String retornoPanel, String activeKey, Map<String, String> keys,
        Integer expiryMinutes, boolean allowInsecureLocalhost) {
    @Override
    public String toString() {
        return "OAuthProperties[enabled=" + enabled + ", clientId=" + clientId + ", clientSecret=[REDACTADO], keys=[REDACTADO]]";
    }
    public void validar() {
        if (!enabled) return;
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank())
            throw new IllegalStateException("Faltan credenciales OAuth de Mercado Pago");
        validarUrl(authorizationUrl); validarUrl(tokenUrl); validarUrl(callbackUrl); validarUrl(retornoPanel);
        if (expiryMinutes == null || expiryMinutes < 1 || expiryMinutes > 60) throw new IllegalStateException("Vigencia OAuth inválida");
        if (activeKey == null || keys == null) throw new IllegalStateException("Faltan claves OAuth");
        new com.ecommerce.backend.cuentapago.vault.AesGcmCredentialVault(activeKey, keys);
    }
    private void validarUrl(String value) {
        try {
            URI uri = URI.create(value);
            boolean local = allowInsecureLocalhost && "http".equals(uri.getScheme()) && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
            if ((!"https".equals(uri.getScheme()) && !local) || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null || uri.getQuery() != null)
                throw new IllegalStateException("URL OAuth inválida");
        } catch (NullPointerException | IllegalArgumentException ex) { throw new IllegalStateException("URL OAuth inválida"); }
    }
}
