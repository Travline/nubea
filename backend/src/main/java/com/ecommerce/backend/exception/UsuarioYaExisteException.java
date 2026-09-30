package com.ecommerce.backend.exception;

public class UsuarioYaExisteException extends RuntimeException{
    public UsuarioYaExisteException(String mensaje){
        super(mensaje);
    }
    
}
