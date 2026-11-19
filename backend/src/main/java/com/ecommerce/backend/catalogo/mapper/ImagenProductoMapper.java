
package com.ecommerce.backend.catalogo.mapper;

import com.ecommerce.backend.catalogo.dto.response.ImagenProductoResponse;
import com.ecommerce.backend.catalogo.model.ImagenProducto;

public class ImagenProductoMapper {

    public static ImagenProductoResponse toResponse(ImagenProducto imagen, String url) {

        if (imagen == null) {
            return null;
        }

        ImagenProductoResponse response = new ImagenProductoResponse();

        response.setIdImagen(imagen.getIdImagen());
        response.setUrl(url);
        response.setOrden(imagen.getOrden());
        response.setEsPrincipal(imagen.getEsPrincipal());

        return response;
    }
}
