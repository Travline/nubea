package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.repository.ProductoRepository;
import com.ecommerce.backend.catalogo.repository.CategoriaRepository;


import java.util.UUID;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.backend.catalogo.dto.request.ProductoRequest;
import com.ecommerce.backend.catalogo.dto.response.ProductoResponse;
import com.ecommerce.backend.catalogo.mapper.ProductoMapper;
import com.ecommerce.backend.catalogo.model.Producto;
import org.springframework.stereotype.Service;

@Service

public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    

   public ProductoService(
        ProductoRepository productoRepository,
        CategoriaRepository categoriaRepository) {
    this.productoRepository = productoRepository;
    this.categoriaRepository = categoriaRepository;
}

    @Transactional
    public ProductoResponse crearProducto(
            UUID idTienda,
            ProductoRequest request) {
        // Validar la solicitud
        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud del producto no puede estar vacía");
        }

        // Verificar que la categoría pertenezca a la tienda
        categoriaRepository.findByIdCategoriaAndIdTienda(
                request.getIdCategoria(),
                idTienda).orElseThrow(
                        () -> new IllegalArgumentException(
                                "La categoría no existe en esta tienda"));

        // Crear el producto
        Producto producto = new Producto();

        producto.setIdTienda(idTienda);
        producto.setIdCategoria(request.getIdCategoria());
        producto.setNombre(request.getNombre().trim());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecioBase(request.getPrecioBase());
        producto.setEstado("BORRADOR");

        // Guardar el producto
        Producto guardado = productoRepository.save(producto);

        return ProductoMapper.toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarProductos(UUID idTienda) {

        return productoRepository.findByIdTienda(idTienda)
                .stream()
                .map(ProductoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerProductoPorId(
            UUID idProducto,
            UUID idTienda) {
        Producto producto = productoRepository
                .findByIdProductoAndIdTienda(idProducto, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto no existe en esta tienda"));

        return ProductoMapper.toResponse(producto);
    }

    @Transactional
    public ProductoResponse actualizarProducto(
            UUID idProducto,
            UUID idTienda,
            ProductoRequest request) {
        // Validar la solicitud
        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud del producto no puede estar vacía");
        }

        // Buscar el producto dentro de la tienda
        Producto producto = productoRepository
                .findByIdProductoAndIdTienda(idProducto, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto no existe en esta tienda"));

        // Verificar que la categoría pertenezca a la tienda
        categoriaRepository.findByIdCategoriaAndIdTienda(
                request.getIdCategoria(),
                idTienda).orElseThrow(
                        () -> new IllegalArgumentException(
                                "La categoría no existe en esta tienda"));

        // Actualizar los datos del producto
        producto.setIdCategoria(request.getIdCategoria());
        producto.setNombre(request.getNombre().trim());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecioBase(request.getPrecioBase());

        // Guardar los cambios
        Producto actualizado = productoRepository.save(producto);

        return ProductoMapper.toResponse(actualizado);
    }

    @Transactional
    public ProductoResponse cambiarEstadoProducto(
            UUID idProducto,
            UUID idTienda,
            String nuevoEstado) {
        // Validar el estado recibido
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new IllegalArgumentException(
                    "El estado del producto no puede estar vacío");
        }

        String estado = nuevoEstado.trim().toUpperCase();

        if (!estado.equals("BORRADOR")
                && !estado.equals("PUBLICADO")
                && !estado.equals("INACTIVO")) {
            throw new IllegalArgumentException(
                    "El estado indicado no es válido");
        }

        // Buscar el producto dentro de la tienda
        Producto producto = productoRepository
                .findByIdProductoAndIdTienda(idProducto, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto no existe en esta tienda"));

        // Actualizar el estado
        producto.setEstado(estado);

        Producto actualizado = productoRepository.save(producto);

        return ProductoMapper.toResponse(actualizado);
    }

}