
package com.ecommerce.backend.catalogo.mapper;

import com.ecommerce.backend.catalogo.dto.response.ImagenProductoResponse;
import com.ecommerce.backend.catalogo.model.ImagenProducto;

public class ImagenProductoMapper {

    public static ImagenProductoResponse toResponse(ImagenProducto imagen) {

        if (imagen == null) {
            return null;
        }

        ImagenProductoResponse response = new ImagenProductoResponse();

        response.setIdImagen(imagen.getIdImagen());
        response.setStorageKey(imagen.getStorageKey());
        response.setOrden(imagen.getOrden());
        response.setEsPrincipal(imagen.getEsPrincipal());

        return response;
    }
}
