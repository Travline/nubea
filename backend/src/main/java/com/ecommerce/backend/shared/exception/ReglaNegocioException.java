package com.ecommerce.backend.shared.exception;

/** Para una regla incumplida, responde 422. Si se indica {@code campo}, la respuesta lo incluye en {@code campos}. */
public class ReglaNegocioException extends RuntimeException {
    private final String campo;

    public ReglaNegocioException(String mensaje) {
        this(null, mensaje);
    }

    public ReglaNegocioException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
