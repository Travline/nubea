package com.ecommerce.backend.plantilla.service;

import com.ecommerce.backend.plantilla.dto.ActualizarEstadoPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.ActualizarPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.CrearPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.PlantillaResponse;

import java.util.List;

public interface PlantillaService {
    PlantillaResponse crearPlantilla(CrearPlantillaRequest request);
    PlantillaResponse actualizarPlantilla(Short idPlantilla, ActualizarPlantillaRequest request);
    PlantillaResponse actualizarEstadoPlantilla(Short idPlantilla, ActualizarEstadoPlantillaRequest request);
    List<PlantillaResponse> listarPlantillas(Boolean incluirInactivas);
    PlantillaResponse obtenerPorId(Short idPlantilla);
}
