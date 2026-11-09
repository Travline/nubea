package com.ecommerce.backend.cuentapago.oauth;

public class OAuthCuentaActivaException extends OAuthConflictoException {
    public OAuthCuentaActivaException() { super("La cuenta de pago ya está activa"); }
}
