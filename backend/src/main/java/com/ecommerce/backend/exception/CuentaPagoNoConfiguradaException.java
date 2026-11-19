package com.ecommerce.backend.exception;

public class CuentaPagoNoConfiguradaException extends RuntimeException {
    public CuentaPagoNoConfiguradaException() {
        super("La cuenta de pago de la tienda no está configurada");
    }
}
