package com.ecommerce.backend.cuentapago.oauth;
public class OAuthEstadoInvalidoException extends RuntimeException {
    public OAuthEstadoInvalidoException() { super("Estado OAuth inválido o vencido"); }
}
