package com.ecommerce.backend.cuentapago.oauth;

public interface MercadoPagoOAuthPort {
    String urlAutorizacion(String state, String challenge);
    Credenciales canjear(String code, String verifier);
    record Credenciales(String idCuenta, String accessToken, String refreshToken, Long expiresIn) {
        @Override
        public String toString() { return "Credenciales[idCuenta=" + idCuenta + ", tokens=[REDACTADOS], expiresIn=" + expiresIn + "]"; }
    }
}
