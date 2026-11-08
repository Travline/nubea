
package com.ecommerce.backend.catalogo.mapper;

import com.ecommerce.backend.catalogo.dto.response.ProductoResponse;
import com.ecommerce.backend.catalogo.model.Producto;

public class ProductoMapper {

    public static ProductoResponse toResponse(Producto producto) {

        if (producto == null) {
            return null;
        }

        ProductoResponse response = new ProductoResponse();

        response.setIdProducto(producto.getIdProducto());
        response.setIdCategoria(producto.getIdCategoria());
        response.setNombre(producto.getNombre());
        response.setDescripcion(producto.getDescripcion());
        response.setPrecioBase(producto.getPrecioBase());
        response.setEstado(producto.getEstado());
        response.setFechaCreacion(producto.getFechaCreacion());
        response.setFechaActualizacion(producto.getFechaActualizacion());

        return response;
    }
}
