
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.repository.ProductoRepository;
import com.ecommerce.backend.catalogo.repository.CategoriaRepository;
import com.ecommerce.backend.catalogo.dto.request.ProductoRequest;
import com.ecommerce.backend.catalogo.dto.response.ProductoResponse;
import com.ecommerce.backend.catalogo.mapper.ProductoMapper;
import com.ecommerce.backend.catalogo.model.Producto;
import com.ecommerce.backend.tienda.repository.TiendaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Locale;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final TiendaRepository tiendaRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            TiendaRepository tiendaRepository) {

        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.tiendaRepository = tiendaRepository;
    }

    // Validar nombre del producto
    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del producto es obligatorio");
        }

        return nombre.trim();
    }

    // Validar que la tienda exista
    private void validarTienda(UUID idTienda) {
        if (idTienda == null) {
            throw new IllegalArgumentException(
                    "La tienda es obligatoria");
        }

        tiendaRepository.findById(idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La tienda no existe"));
    }

    // Validar que la categoría pertenezca a la tienda
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

    // Buscar producto dentro de una tienda
    private Producto buscarProducto(UUID idProducto, UUID idTienda) {
        return productoRepository
                .findByIdProductoAndIdTienda(idProducto, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto no existe en esta tienda"));
    }

    // Crear producto
    @Transactional
    public ProductoResponse crearProducto(
            UUID idTienda,
            ProductoRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud del producto no puede estar vacía");
        }

        validarTienda(idTienda);

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

    // Listar productos de una tienda
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarProductos(UUID idTienda) {

        validarTienda(idTienda);

        return productoRepository.findByIdTienda(idTienda)
                .stream()
                .map(ProductoMapper::toResponse)
                .toList();
    }

    // Obtener producto por ID
    @Transactional(readOnly = true)
    public ProductoResponse obtenerProductoPorId(
            UUID idProducto,
            UUID idTienda) {

        validarTienda(idTienda);

        Producto producto = buscarProducto(idProducto, idTienda);

        return ProductoMapper.toResponse(producto);
    }

    // Actualizar producto
    @Transactional
    public ProductoResponse actualizarProducto(
            UUID idProducto,
            UUID idTienda,
            ProductoRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud del producto no puede estar vacía");
        }

        validarTienda(idTienda);

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

    // Cambiar estado del producto
    @Transactional
    public ProductoResponse cambiarEstadoProducto(
            UUID idProducto,
            UUID idTienda,
            String nuevoEstado) {

        validarTienda(idTienda);

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
