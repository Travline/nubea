package com.ecommerce.backend.cuentapago;

import com.ecommerce.backend.cuentapago.oauth.OAuthProperties;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OAuthPropertiesTest {
    private OAuthProperties config(String callback, boolean local, String key, int minutes) {
        return new OAuthProperties(true, "id", "secreto", "https://auth.example", "https://api.example/token", callback,
                "https://panel.example", "k", Map.of("k", key), minutes, local);
    }
    @Test
    void exigeHttpsVigenciaYClaveValida() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        assertDoesNotThrow(() -> config("https://backend.example/callback", false, key, 10).validar());
        assertFalse(config("https://backend.example/callback", false, key, 10).toString().contains("secreto"));
        assertFalse(config("https://backend.example/callback", false, key, 10).toString().contains(key));
        var credenciales = new com.ecommerce.backend.cuentapago.oauth.MercadoPagoOAuthPort.Credenciales("1", "token-secreto", "refresh-secreto", 60L);
        assertFalse(credenciales.toString().contains("secreto"));
        assertThrows(IllegalStateException.class, () -> config("http://backend.example/callback", false, key, 10).validar());
        assertThrows(IllegalStateException.class, () -> config("https://backend.example/callback?state=x", false, key, 10).validar());
        assertThrows(IllegalStateException.class, () -> config("https://backend.example/callback", false, key, 0).validar());
        assertThrows(IllegalArgumentException.class, () -> config("https://backend.example/callback", false, "YQ==", 10).validar());
        assertDoesNotThrow(() -> config("http://localhost:8080/callback", true, key, 10).validar());
    }
}
