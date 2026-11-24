package com.ecommerce.backend.identidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRequest(
    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden superar 100 caracteres")
    String nombres,

    @Size(max = 80, message = "El apellido paterno no puede superar 80 caracteres")
    String apellidoPaterno,

    @Size(max = 80, message = "El apellido materno no puede superar 80 caracteres")
    String apellidoMaterno,

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    @Pattern(regexp = "^$|^[+0-9 ()-]{6,20}$", message = "El teléfono solo puede tener números, espacios, +, ( ) y -")
    String telefono,

    String tipoDocumento,

    @Size(max = 30, message = "El número de documento no puede superar 30 caracteres")
    String numeroDocumento,

    @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "El país emisor debe ser un código de dos letras (por ejemplo, CO)")
    String paisEmisorDocumento
) {}
