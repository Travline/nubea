package com.ecommerce.backend.plantilla.dto;

import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoPlantillaRequest(
    @NotNull(message = "El estado 'activa' es obligatorio")
    Boolean activa
) {}
