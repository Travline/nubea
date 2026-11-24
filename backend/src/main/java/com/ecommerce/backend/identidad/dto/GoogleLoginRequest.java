package com.ecommerce.backend.identidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** ID token de Google Identity Services ({@code credential}). {@code recordarme} es opcional, como en el login local. */
public record GoogleLoginRequest(
        @NotBlank(message = "Falta el token de Google") @Size(max = 8192, message = "Token de Google no válido") String idToken,
        Boolean recordarme) {
    @Override
    public String toString() {
        return "GoogleLoginRequest[idToken=***, recordarme=" + recordarme + "]";
    }
}
