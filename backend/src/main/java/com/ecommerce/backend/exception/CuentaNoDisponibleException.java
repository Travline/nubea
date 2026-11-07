package com.ecommerce.backend.exception;

public class CuentaNoDisponibleException extends RuntimeException {
    
    public CuentaNoDisponibleException(String mensaje){
    super(mensaje);
    }
}
