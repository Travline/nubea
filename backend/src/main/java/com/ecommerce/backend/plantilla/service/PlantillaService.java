package com.ecommerce.backend.plantilla.service;

import com.ecommerce.backend.plantilla.dto.ActualizarEstadoPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.ActualizarPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.CrearPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.PlantillaResponse;

public interface PlantillaService {
    PlantillaResponse crearPlantilla(CrearPlantillaRequest request);
    PlantillaResponse actualizarPlantilla(Short idPlantilla, ActualizarPlantillaRequest request);
    PlantillaResponse actualizarEstadoPlantilla(Short idPlantilla, ActualizarEstadoPlantillaRequest request);
}
