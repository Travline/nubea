package com.ecommerce.backend.plantilla.dto;

import jakarta.validation.constraints.Size;

public record ActualizarPlantillaRequest(
    @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
    String nombre,

    @Size(max = 2048, message = "La URL de vista previa no debe superar 2048 caracteres")
    String previewUrl,

    Boolean activa
) {}
