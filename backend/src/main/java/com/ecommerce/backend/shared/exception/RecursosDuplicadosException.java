package com.ecommerce.backend.shared.exception;

/** Responde 409 DUPLICADO. Si se indica {@code campo}, la respuesta lo incluye en {@code campos}. */
public class RecursosDuplicadosException extends RuntimeException {
    private final String campo;

    public RecursosDuplicadosException(String mensaje) {
        this(null, mensaje);
    }

    public RecursosDuplicadosException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
