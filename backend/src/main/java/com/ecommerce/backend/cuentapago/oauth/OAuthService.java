package com.ecommerce.backend.cuentapago.oauth;

import com.ecommerce.backend.cuentapago.vault.PaymentCredentialVault;
import com.ecommerce.backend.shared.security.CurrentUserProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mercadopago.oauth", name = "enabled", havingValue = "true")
public class OAuthService {
    private final OAuthTransactionStore store;
    private final MercadoPagoOAuthPort proveedor;
    private final PaymentCredentialVault vault;
    private final OAuthProperties config;
    private final CurrentUserProvider currentUser;
    private final SecureRandom random = new SecureRandom();

    public String iniciar(UUID tienda) {
        UUID usuario = currentUser.getCurrentUserId().orElseThrow(OAuthEstadoInvalidoException::new);
        UUID id = UUID.randomUUID();
        String state = generar();
        String verifier = generar();
        String url = proveedor.urlAutorizacion(state, base64(hash(verifier)));
        store.iniciar(id, tienda, usuario, hash(state), vault.cifrar(verifier, "oauth-verifier:" + id), config.retornoPanel(), config.expiryMinutes());
        return url;
    }

    public String callback(String state, String code, String error) {
        if (state == null || !state.matches("[A-Za-z0-9_-]{43}")) throw new OAuthEstadoInvalidoException();
        var intento = store.consumir(hash(state));
        String resultado = "error";
        try {
            if (error == null && code != null && !code.isBlank() && code.length() <= 4096) {
                store.validar(intento);
                String verifier = vault.descifrar(intento.verifier(), "oauth-verifier:" + intento.id());
                var credenciales = proveedor.canjear(code, verifier);
                String datos = base64(credenciales.accessToken().getBytes(StandardCharsets.UTF_8)) + "."
                        + base64(credenciales.refreshToken().getBytes(StandardCharsets.UTF_8)) + "."
                        + (credenciales.expiresIn() == null ? "" : java.time.Instant.now().plusSeconds(credenciales.expiresIn()).toString());
                store.activar(intento, credenciales.idCuenta(), vault.cifrar(datos, "mercadopago:" + intento.tienda()));
                resultado = "exito";
            }
        } catch (RuntimeException ex) {
            if (esCuentaDuplicada(ex)) resultado = "conflicto";
            // Nunca propagar al navegador detalles del proveedor, SQL o credenciales.
        }
        return intento.retorno() + (intento.retorno().contains("?") ? "&" : "?") + "resultado=" + resultado;
    }
    private static boolean esCuentaDuplicada(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) return true;
        }
        return false;
    }
    private String generar() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return base64(bytes); }
    private static String base64(byte[] bytes) { return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static byte[] hash(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
