package com.ecommerce.backend.plantilla.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearPlantillaRequest(
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 60, message = "El código no debe superar 60 caracteres")
    String codigo,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
    String nombre,

    @Size(max = 2048, message = "La URL de vista previa no debe superar 2048 caracteres")
    String previewUrl,

    Boolean activa
) {
    public boolean activaOrDefault() {
        return activa != null ? activa : Boolean.TRUE;
    }
}
