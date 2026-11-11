package com.ecommerce.backend.shared.exception;

public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje){
        super(mensaje);
    }
    //Para una regla incumplida, responde 422
    
}
