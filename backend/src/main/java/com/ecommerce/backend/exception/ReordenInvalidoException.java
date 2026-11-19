package com.ecommerce.backend.exception;

public class ReordenInvalidoException extends RuntimeException {
    public ReordenInvalidoException(String mensaje) {
        super(mensaje);
    }
}
