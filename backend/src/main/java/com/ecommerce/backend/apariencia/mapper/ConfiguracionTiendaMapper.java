package com.ecommerce.backend.apariencia.mapper;

import com.ecommerce.backend.apariencia.dto.ConfiguracionTiendaResponse;
import com.ecommerce.backend.apariencia.model.ConfiguracionTienda;
import com.ecommerce.backend.plantilla.mapper.PlantillaMapper;

public final class ConfiguracionTiendaMapper {

    private ConfiguracionTiendaMapper() {
        // Constructor privado para evitar instanciación
    }

    public static ConfiguracionTiendaResponse toResponse(ConfiguracionTienda configuracion) {
        if (configuracion == null) {
            return null;
        }
        return new ConfiguracionTiendaResponse(
            configuracion.getIdTienda(),
            PlantillaMapper.toResponse(configuracion.getPlantilla()),
            configuracion.getTituloInicio(),
            configuracion.getDescripcionInicio(),
            configuracion.getMostrarCarrusel(),
            configuracion.getFechaActualizacion()
        );
    }
}
