package com.ecommerce.backend.shared.exception;

public class RecursosDuplicadosException extends RuntimeException {
    public RecursosDuplicadosException(String mensaje)
    {
        super(mensaje);
    }
    //Para lo que existe, responde 409
}
