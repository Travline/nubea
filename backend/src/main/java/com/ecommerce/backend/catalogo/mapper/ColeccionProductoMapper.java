
package com.ecommerce.backend.catalogo.mapper;

import com.ecommerce.backend.catalogo.dto.response.ColeccionProductoResponse;
import com.ecommerce.backend.catalogo.model.ColeccionProducto;

public class ColeccionProductoMapper {

    public static ColeccionProductoResponse toResponse(
            ColeccionProducto relacion) {

        if (relacion == null) {
            return null;
        }

        ColeccionProductoResponse response =
                new ColeccionProductoResponse();

        response.setIdColeccion(
                relacion.getId().getIdColeccion());

        response.setIdProducto(
                relacion.getId().getIdProducto());

        return response;
    }
}
