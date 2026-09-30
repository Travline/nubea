package com.ecommerce.backend.exception;

public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje){
        super(mensaje);
    }
    
}
