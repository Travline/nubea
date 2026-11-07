package com.ecommerce.backend.plantilla.service;

import com.ecommerce.backend.plantilla.dto.CrearPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.PlantillaResponse;

public interface PlantillaService {
    PlantillaResponse crearPlantilla(CrearPlantillaRequest request);
}
