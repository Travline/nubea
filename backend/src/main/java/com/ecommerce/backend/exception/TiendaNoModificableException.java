package com.ecommerce.backend.exception;

public class TiendaNoModificableException extends RuntimeException {
    public TiendaNoModificableException(String mensaje) {
        super(mensaje);
    }
}
