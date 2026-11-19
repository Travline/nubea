package com.ecommerce.backend.cuentapago.vault;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Sobre v1 con identificador de clave, nonce y texto cifrado autenticado. */
public final class AesGcmCredentialVault implements PaymentCredentialVault {
    private final String activeKey;
    private final Map<String, String> keys;
    private final SecureRandom random = new SecureRandom();

    public AesGcmCredentialVault(String activeKey, Map<String, String> keys) {
        this.keys = Map.copyOf(keys);
        if (activeKey == null || !this.keys.containsKey(activeKey)) throw new IllegalArgumentException("Clave activa inválida");
        this.keys.forEach((id, value) -> {
            if (!id.matches("[A-Za-z0-9_-]{1,64}") || Base64.getDecoder().decode(value).length != 32)
                throw new IllegalArgumentException("Clave de cifrado inválida");
        });
        this.activeKey = activeKey;
    }

    @Override
    public String cifrar(String secreto, String finalidad) {
        try {
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(activeKey), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(finalidad.getBytes(StandardCharsets.UTF_8));
            var encoder = Base64.getUrlEncoder().withoutPadding();
            return "v1." + activeKey + "." + encoder.encodeToString(nonce) + "." + encoder.encodeToString(cipher.doFinal(secreto.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo proteger la credencial", e);
        }
    }

    @Override
    public String descifrar(String sobre, String finalidad) {
        try {
            String[] partes = sobre.split("\\.", -1);
            if (partes.length != 4 || !partes[0].equals("v1") || !keys.containsKey(partes[1])) throw new IllegalArgumentException();
            byte[] nonce = Base64.getUrlDecoder().decode(partes[2]);
            if (nonce.length != 12) throw new IllegalArgumentException();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(partes[1]), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(finalidad.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(Base64.getUrlDecoder().decode(partes[3])), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("No se pudo verificar la credencial cifrada", e);
        }
    }

    private SecretKeySpec key(String id) { return new SecretKeySpec(Base64.getDecoder().decode(keys.get(id)), "AES"); }
}
