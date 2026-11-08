
package com.ecommerce.backend.catalogo.mapper;

import com.ecommerce.backend.catalogo.dto.response.VarianteResponse;
import com.ecommerce.backend.catalogo.model.Variante;

public class VarianteMapper {

    public static VarianteResponse toResponse(Variante variante) {

        if (variante == null) {
            return null;
        }

        VarianteResponse response = new VarianteResponse();

        response.setIdVariante(variante.getIdVariante());
        response.setNombre(variante.getNombre());

        return response;
    }
}
