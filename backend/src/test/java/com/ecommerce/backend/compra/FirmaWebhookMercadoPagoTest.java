package com.ecommerce.backend.compra;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class FirmaWebhookMercadoPagoTest {
    private static final String FIRMA = "ts=1700000000,v1=ea45cb42eb8500f79ad9371c021e6a346f935319568fde7ccf4a2387fc50ee7e";

    @Test
    void validaFirmaYNormalizaId() {
        assertTrue(FirmaWebhookMercadoPago.verificar("clave-prueba", FIRMA, "req-123", "ORD123"));
        assertFalse(FirmaWebhookMercadoPago.verificar("otra-clave", FIRMA, "req-123", "ORD123"));
        assertFalse(FirmaWebhookMercadoPago.verificar("clave-prueba", FIRMA, "otro", "ORD123"));
        assertFalse(FirmaWebhookMercadoPago.verificar("clave-prueba", FIRMA, "req-123", "ORD124"));
    }

    @Test
    void rechazaDatosIncompletosOMalformados() {
        assertFalse(FirmaWebhookMercadoPago.verificar("", FIRMA, "req-123", "ORD123"));
        assertFalse(FirmaWebhookMercadoPago.verificar("clave-prueba", "ts=1,v1=xyz", "req-123", "ORD123"));
        assertFalse(FirmaWebhookMercadoPago.verificar("clave-prueba", FIRMA + ",ts=2", "req-123", "ORD123"));
        assertFalse(FirmaWebhookMercadoPago.verificar("clave-prueba", FIRMA, null, "ORD123"));
        assertFalse(FirmaWebhookMercadoPago.verificar("clave-prueba", FIRMA, "req-123", "ORD;123"));
    }
}
