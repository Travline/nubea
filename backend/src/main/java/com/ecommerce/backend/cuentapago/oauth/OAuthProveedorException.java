package com.ecommerce.backend.cuentapago.oauth;

public class OAuthProveedorException extends RuntimeException {
    public OAuthProveedorException() { super("No se pudo completar la autorización con Mercado Pago"); }
}
