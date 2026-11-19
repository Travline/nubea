
package com.ecommerce.backend.catalogo.mapper;

import com.ecommerce.backend.catalogo.dto.response.CategoriaResponse;
import com.ecommerce.backend.catalogo.model.Categoria;

public class CategoriaMapper {

    public static CategoriaResponse toResponse(Categoria categoria) {

        if (categoria == null) {
            return null;
        }

        CategoriaResponse response = new CategoriaResponse();

        response.setIdCategoria(categoria.getIdCategoria());
        response.setNombre(categoria.getNombre());
        response.setSlug(categoria.getSlug());
        response.setActiva(categoria.getActiva());

        return response;
    }
}
