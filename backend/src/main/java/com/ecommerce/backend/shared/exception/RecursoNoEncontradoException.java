package com.ecommerce.backend.shared.exception;

public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje){
       super(mensaje); 
    }
    //Para lo que no existe, responde 404
}
