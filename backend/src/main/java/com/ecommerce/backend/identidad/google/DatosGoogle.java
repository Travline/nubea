package com.ecommerce.backend.identidad.google;

/** Datos verificados de un ID token de Google. {@code issuer} siempre normalizado a {@link GoogleIdTokenVerifier#ISSUER}. */
public record DatosGoogle(String issuer, String subject, String email, String nombres, String apellido) {
    /** Nunca exponer claims completos en logs: como mucho el subject. */
    @Override
    public String toString() {
        return "DatosGoogle[sub=" + subject + "]";
    }
}
