
package com.ecommerce.backend.catalogo.mapper;

import com.ecommerce.backend.catalogo.dto.response.ValorVarianteResponse;
import com.ecommerce.backend.catalogo.model.ValorVariante;

public class ValorVarianteMapper {

    public static ValorVarianteResponse toResponse(ValorVariante valorVariante) {

        if (valorVariante == null) {
            return null;
        }

        ValorVarianteResponse response = new ValorVarianteResponse();

        response.setIdValor(valorVariante.getIdValor());
        response.setValor(valorVariante.getValor());

        return response;
    }
}
