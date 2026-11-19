
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
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final TiendaRepository tiendaRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            TiendaRepository tiendaRepository,
            JdbcTemplate jdbcTemplate) {

        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.tiendaRepository = tiendaRepository;
        this.jdbcTemplate = jdbcTemplate;
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

        Producto producto = buscarProducto(idProducto, idTienda);

        if (estado.equals("PUBLICADO")) {
            Boolean publicable = jdbcTemplate.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM producto p
                        JOIN tienda t ON t.id_tienda = p.id_tienda
                        JOIN categoria c ON c.id_categoria = p.id_categoria AND c.id_tienda = p.id_tienda
                        JOIN presentacion_producto pp ON pp.id_producto = p.id_producto AND pp.id_tienda = p.id_tienda
                        JOIN inventario i ON i.id_presentacion = pp.id_presentacion
                        WHERE p.id_producto = ? AND p.id_tienda = ? AND t.estado = 'ACTIVA'
                          AND c.activa = true AND pp.estado = 'ACTIVA'
                    )
                    """, Boolean.class, idProducto, idTienda);
            if (!Boolean.TRUE.equals(publicable)) {
                throw new IllegalArgumentException("El producto requiere tienda y categoría activas y una presentación activa con inventario");
            }
        }

        producto.setEstado(estado);

        Producto actualizado = productoRepository.save(producto);

        return ProductoMapper.toResponse(actualizado);
    }
}
