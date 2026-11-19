package com.ecommerce.backend.cuentapago;

import com.ecommerce.backend.cuentapago.vault.AesGcmCredentialVault;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AesGcmCredentialVaultTest {
    @Test
    void protegeDatosValidaContextoYPermiteRotacion() {
        String anterior = Base64.getEncoder().encodeToString(new byte[32]);
        byte[] bytes = new byte[32]; bytes[0] = 1;
        String nueva = Base64.getEncoder().encodeToString(bytes);
        var viejo = new AesGcmCredentialVault("k1", Map.of("k1", anterior));
        var rotado = new AesGcmCredentialVault("k2", Map.of("k1", anterior, "k2", nueva));
        String sobre = viejo.cifrar("token sensible", "tienda:1");
        assertFalse(sobre.contains("token sensible"));
        assertEquals("token sensible", rotado.descifrar(sobre, "tienda:1"));
        assertThrows(IllegalArgumentException.class, () -> rotado.descifrar(sobre, "tienda:2"));
        assertNotEquals(sobre, viejo.cifrar("token sensible", "tienda:1"));
        assertTrue(rotado.cifrar("x", "tienda:1").startsWith("v1.k2."));
    }
}
