
package com.ecommerce.backend.catalogo.mapper;

import com.ecommerce.backend.catalogo.dto.response.ColeccionResponse;
import com.ecommerce.backend.catalogo.model.Coleccion;

public class ColeccionMapper {

    public static ColeccionResponse toResponse(Coleccion coleccion) {

        if (coleccion == null) {
            return null;
        }

        ColeccionResponse response = new ColeccionResponse();

        response.setIdColeccion(coleccion.getIdColeccion());
        response.setNombre(coleccion.getNombre());
        response.setSlug(coleccion.getSlug());
        response.setActiva(coleccion.getActiva());

        return response;
    }
}
