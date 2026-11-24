package com.ecommerce.backend.shared.exception;

/** 409 CONFLICTO ligado a un campo: el estado actual impide la operación (p. ej. el stock cambió). */
public class ConflictoCampoException extends RuntimeException {
    private final String campo;

    public ConflictoCampoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
