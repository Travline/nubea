
package com.ecommerce.backend.catalogo.mapper;

import com.ecommerce.backend.catalogo.dto.response.PresentacionProductoResponse;
import com.ecommerce.backend.catalogo.model.PresentacionProducto;

public class PresentacionProductoMapper {

    public static PresentacionProductoResponse toResponse(
            PresentacionProducto presentacion) {

        if (presentacion == null) {
            return null;
        }

        PresentacionProductoResponse response =
                new PresentacionProductoResponse();

        response.setIdPresentacion(presentacion.getIdPresentacion());
        response.setIdProducto(presentacion.getIdProducto());
        response.setSku(presentacion.getSku());
        response.setPrecioOverride(presentacion.getPrecioOverride());
        response.setEstado(presentacion.getEstado());

        return response;
    }
}
