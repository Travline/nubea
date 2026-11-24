package com.ecommerce.backend.identidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarContrasenaRequest(
    @NotBlank(message = "Ingresa tu contraseña actual")
    String contrasenaActual,

    @NotBlank(message = "Ingresa la nueva contraseña")
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    String contrasenaNueva
) {}
