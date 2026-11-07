package com.ecommerce.backend.apariencia.dto;

import com.ecommerce.backend.plantilla.dto.PlantillaResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConfiguracionTiendaResponse(
    UUID idTienda,
    PlantillaResponse plantilla,
    String tituloInicio,
    String descripcionInicio,
    Boolean mostrarCarrusel,
    OffsetDateTime fechaActualizacion
) {}
