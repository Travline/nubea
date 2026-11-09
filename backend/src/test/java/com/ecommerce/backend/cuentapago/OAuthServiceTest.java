package com.ecommerce.backend.cuentapago;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ecommerce.backend.cuentapago.oauth.*;
import com.ecommerce.backend.cuentapago.vault.AesGcmCredentialVault;
import com.ecommerce.backend.shared.security.CurrentUserProvider;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OAuthServiceTest {
    @Test
    void pkceYCallbackSoloActivanConRespuestaValida() {
        var store = mock(OAuthTransactionStore.class);
        var port = mock(MercadoPagoOAuthPort.class);
        var user = mock(CurrentUserProvider.class);
        var vault = new AesGcmCredentialVault("k", Map.of("k", Base64.getEncoder().encodeToString(new byte[32])));
        var config = new OAuthProperties(true, "id", "secret", "https://auth.example", "https://api.example", "https://api.example/cb", "https://panel.example/return", "k", Map.of("k", Base64.getEncoder().encodeToString(new byte[32])), 10, false);
        var service = new OAuthService(store, port, vault, config, user);
        UUID tienda = UUID.randomUUID(), usuario = UUID.randomUUID();
        when(user.getCurrentUserId()).thenReturn(Optional.of(usuario));
        when(port.urlAutorizacion(anyString(), anyString())).thenReturn("https://auth.example");
        service.iniciar(tienda);
        var state = ArgumentCaptor.forClass(String.class);
        var challenge = ArgumentCaptor.forClass(String.class);
        verify(port).urlAutorizacion(state.capture(), challenge.capture());
        var id = ArgumentCaptor.forClass(UUID.class);
        var encrypted = ArgumentCaptor.forClass(String.class);
        verify(store).iniciar(id.capture(), eq(tienda), eq(usuario), any(byte[].class), encrypted.capture(), eq(config.retornoPanel()), eq(10));
        String verifier = vault.descifrar(encrypted.getValue(), "oauth-verifier:" + id.getValue());
        try {
            String expected = Base64.getUrlEncoder().withoutPadding().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
            assertEquals(expected, challenge.getValue());
        } catch (java.security.NoSuchAlgorithmException ex) { fail(ex); }
        var attempt = new OAuthTransactionStore.Intento(id.getValue(), tienda, usuario, encrypted.getValue(), config.retornoPanel());
        when(store.consumir(any(byte[].class))).thenReturn(attempt);
        when(port.canjear("code", verifier)).thenReturn(new MercadoPagoOAuthPort.Credenciales("123", "access", "refresh", 3600L));
        assertEquals("https://panel.example/return?resultado=exito", service.callback(state.getValue(), "code", null));
        var saved = ArgumentCaptor.forClass(String.class);
        verify(store).activar(eq(attempt), eq("123"), saved.capture());
        assertFalse(saved.getValue().contains("access"));
        assertTrue(vault.descifrar(saved.getValue(), "mercadopago:" + tienda).startsWith("YWNjZXNz.cmVmcmVzaA."));
        assertEquals("https://panel.example/return?resultado=error", service.callback(state.getValue(), null, "access_denied"));
        verify(port, times(1)).canjear(anyString(), anyString());
        when(port.canjear("failure", verifier)).thenThrow(new OAuthProveedorException());
        assertEquals("https://panel.example/return?resultado=error", service.callback(state.getValue(), "failure", null));
        verify(store, times(1)).activar(any(), anyString(), anyString());
        assertThrows(OAuthEstadoInvalidoException.class, () -> service.callback("mal", "code", null));
    }
}
