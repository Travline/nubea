
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.repository.ProductoRepository;
import com.ecommerce.backend.catalogo.repository.CategoriaRepository;
import com.ecommerce.backend.catalogo.dto.request.ProductoRequest;
import com.ecommerce.backend.catalogo.dto.response.ProductoResponse;
import com.ecommerce.backend.catalogo.mapper.ProductoMapper;
import com.ecommerce.backend.catalogo.model.Producto;

import java.util.UUID;
import java.util.List;
import java.util.Locale;

import org.springframework.transaction.annotation.Transactional;
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

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del producto es obligatorio");
        }

        String nombreLimpio = nombre.trim();

        // Verificar la longitud según el modelo SQL
        return nombreLimpio;
    }

    private void validarCategoria(UUID idCategoria, UUID idTienda) {
        if (idCategoria == null) {
            throw new IllegalArgumentException(
                    "La categoría del producto es obligatoria");
        }

        categoriaRepository.findByIdCategoriaAndIdTienda(
                idCategoria,
                idTienda
        ).orElseThrow(() -> new IllegalArgumentException(
                "La categoría no existe en esta tienda"));
    }

    private Producto buscarProducto(UUID idProducto, UUID idTienda) {
        return productoRepository
                .findByIdProductoAndIdTienda(idProducto, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto no existe en esta tienda"));
    }

    @Transactional
    public ProductoResponse crearProducto(
            UUID idTienda,
            ProductoRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud del producto no puede estar vacía");
        }

        String nombre = validarNombre(request.getNombre());
        validarCategoria(request.getIdCategoria(), idTienda);

        Producto producto = new Producto();

        producto.setIdTienda(idTienda);
        producto.setIdCategoria(request.getIdCategoria());
        producto.setNombre(nombre);
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecioBase(request.getPrecioBase());
        producto.setEstado("BORRADOR");

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

        Producto producto = buscarProducto(idProducto, idTienda);

        return ProductoMapper.toResponse(producto);
    }

    @Transactional
    public ProductoResponse actualizarProducto(
            UUID idProducto,
            UUID idTienda,
            ProductoRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud del producto no puede estar vacía");
        }

        Producto producto = buscarProducto(idProducto, idTienda);

        String nombre = validarNombre(request.getNombre());
        validarCategoria(request.getIdCategoria(), idTienda);

        producto.setIdCategoria(request.getIdCategoria());
        producto.setNombre(nombre);
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecioBase(request.getPrecioBase());

        Producto actualizado = productoRepository.save(producto);

        return ProductoMapper.toResponse(actualizado);
    }

    @Transactional
    public ProductoResponse cambiarEstadoProducto(
            UUID idProducto,
            UUID idTienda,
            String nuevoEstado) {

        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new IllegalArgumentException(
                    "El estado del producto no puede estar vacío");
        }

        String estado = nuevoEstado.trim().toUpperCase(Locale.ROOT);

        if (!estado.equals("BORRADOR")
                && !estado.equals("PUBLICADO")
                && !estado.equals("INACTIVO")) {
            throw new IllegalArgumentException(
                    "El estado indicado no es válido");
        }

        if (estado.equals("PUBLICADO")) {
            throw new IllegalStateException(
                    "La publicación requiere validar la tienda, "
                    + "la categoría, las presentaciones y el inventario");
        }

        Producto producto = buscarProducto(idProducto, idTienda);

        producto.setEstado(estado);

        Producto actualizado = productoRepository.save(producto);

        return ProductoMapper.toResponse(actualizado);
    }
}
