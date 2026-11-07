package com.ecommerce.backend.plantilla.mapper;

import com.ecommerce.backend.plantilla.dto.PlantillaResponse;
import com.ecommerce.backend.plantilla.model.Plantilla;

public final class PlantillaMapper {

    private PlantillaMapper() {
        // Constructor privado para evitar la instanciación
    }

    public static PlantillaResponse toResponse(Plantilla plantilla) {
        if (plantilla == null) {
            return null;
        }
        return new PlantillaResponse(
            plantilla.getIdPlantilla(),
            plantilla.getCodigo(),
            plantilla.getNombre(),
            plantilla.getPreviewUrl(),
            plantilla.getActiva()
        );
    }
}
