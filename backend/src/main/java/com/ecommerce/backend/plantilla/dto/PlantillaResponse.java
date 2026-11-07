package com.ecommerce.backend.plantilla.dto;

public record PlantillaResponse(
    Short idPlantilla,
    String codigo,
    String nombre,
    String previewUrl,
    Boolean activa
) {}
