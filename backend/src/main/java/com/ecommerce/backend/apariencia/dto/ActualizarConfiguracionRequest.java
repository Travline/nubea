package com.ecommerce.backend.apariencia.dto;

import jakarta.validation.constraints.Size;

public record ActualizarConfiguracionRequest(
    Short idPlantilla,

    @Size(max = 160, message = "El título de inicio no debe superar 160 caracteres")
    String tituloInicio,

    String descripcionInicio,

    Boolean mostrarCarrusel
) {}
