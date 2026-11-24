package com.ecommerce.backend.shared.exception;

/**
 * Error de validación (400) ligado a un campo del request. Extiende IllegalArgumentException
 * para conservar la semántica 400 existente, pero la respuesta incluye {@code campos}.
 */
public class ValidacionCampoException extends IllegalArgumentException {
    private final String campo;

    public ValidacionCampoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
