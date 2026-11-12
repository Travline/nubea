
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.dto.request.ImagenProductoRequest;
import com.ecommerce.backend.catalogo.dto.response.ImagenProductoResponse;
import com.ecommerce.backend.catalogo.mapper.ImagenProductoMapper;
import com.ecommerce.backend.catalogo.model.ImagenProducto;
import com.ecommerce.backend.catalogo.repository.ImagenProductoRepository;
import com.ecommerce.backend.catalogo.repository.ProductoRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImagenProductoService {

    private final ImagenProductoRepository imagenProductoRepository;
    private final ProductoRepository productoRepository;

    public ImagenProductoService(
            ImagenProductoRepository imagenProductoRepository,
            ProductoRepository productoRepository) {
        this.imagenProductoRepository = imagenProductoRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public ImagenProductoResponse crearImagen(
            UUID idProducto,
            UUID idTienda,
            ImagenProductoRequest request) {

        validarProducto(idProducto, idTienda);
        validarSolicitud(request);

        if (imagenProductoRepository.existsByStorageKey(
                request.getStorageKey().trim())) {
            throw new IllegalArgumentException(
                    "La clave de almacenamiento ya está registrada");
        }

        validarOrden(request.getOrden());

        boolean esPrincipal = Boolean.TRUE.equals(
                request.getEsPrincipal());

        // Si será principal, quitar esa condición a la anterior.
        if (esPrincipal) {
            imagenProductoRepository
                    .findByIdProductoAndEsPrincipalTrue(idProducto)
                    .ifPresent(imagen -> {
                        imagen.setEsPrincipal(false);
                        imagenProductoRepository.save(imagen);
                    });
        }

        ImagenProducto imagen = new ImagenProducto();

        imagen.setIdProducto(idProducto);
        imagen.setStorageKey(request.getStorageKey().trim());
        imagen.setOrden(request.getOrden());
        imagen.setEsPrincipal(esPrincipal);

        ImagenProducto guardada =
                imagenProductoRepository.save(imagen);

        return ImagenProductoMapper.toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<ImagenProductoResponse> listarImagenes(
            UUID idProducto,
            UUID idTienda) {

        validarProducto(idProducto, idTienda);

        return imagenProductoRepository
                .findByIdProductoOrderByOrdenAsc(idProducto)
                .stream()
                .map(ImagenProductoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImagenProductoResponse obtenerImagenPorId(
            UUID idImagen,
            UUID idProducto,
            UUID idTienda) {

        validarProducto(idProducto, idTienda);

        ImagenProducto imagen = buscarImagen(
                idImagen, idProducto);

        return ImagenProductoMapper.toResponse(imagen);
    }

    @Transactional
    public ImagenProductoResponse actualizarImagen(
            UUID idImagen,
            UUID idProducto,
            UUID idTienda,
            ImagenProductoRequest request) {

        validarProducto(idProducto, idTienda);
        validarSolicitud(request);

        ImagenProducto imagen = buscarImagen(
                idImagen, idProducto);

        String storageKey = request.getStorageKey().trim();

        if (!imagen.getStorageKey().equals(storageKey)
                && imagenProductoRepository.existsByStorageKey(
                        storageKey)) {
            throw new IllegalArgumentException(
                    "La clave de almacenamiento ya está registrada");
        }

        validarOrden(request.getOrden());

        imagen.setStorageKey(storageKey);
        imagen.setOrden(request.getOrden());

        // La imagen principal se gestiona por separado.
        ImagenProducto actualizada =
                imagenProductoRepository.save(imagen);

        return ImagenProductoMapper.toResponse(actualizada);
    }

    @Transactional
    public ImagenProductoResponse establecerImagenPrincipal(
            UUID idImagen,
            UUID idProducto,
            UUID idTienda) {

        validarProducto(idProducto, idTienda);

        ImagenProducto imagen = buscarImagen(
                idImagen, idProducto);

        imagenProductoRepository
                .findByIdProductoAndEsPrincipalTrue(idProducto)
                .ifPresent(actual -> {
                    actual.setEsPrincipal(false);
                    imagenProductoRepository.save(actual);
                });

        imagen.setEsPrincipal(true);

        ImagenProducto actualizada =
                imagenProductoRepository.save(imagen);

        return ImagenProductoMapper.toResponse(actualizada);
    }

    @Transactional
    public void eliminarImagen(
            UUID idImagen,
            UUID idProducto,
            UUID idTienda) {

        validarProducto(idProducto, idTienda);

        ImagenProducto imagen = buscarImagen(
                idImagen, idProducto);

        imagenProductoRepository.delete(imagen);
    }

    // Validar que el producto pertenezca a la tienda.
    private void validarProducto(
            UUID idProducto,
            UUID idTienda) {

        productoRepository
                .findByIdProductoAndIdTienda(
                        idProducto, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto no existe en esta tienda"));
    }

    // Buscar una imagen dentro del producto.
    private ImagenProducto buscarImagen(
            UUID idImagen,
            UUID idProducto) {

        return imagenProductoRepository
                .findByIdImagenAndIdProducto(
                        idImagen, idProducto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La imagen no existe en este producto"));
    }

    private void validarSolicitud(
            ImagenProductoRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud de imagen no puede estar vacía");
        }

        if (request.getStorageKey() == null
                || request.getStorageKey().isBlank()) {
            throw new IllegalArgumentException(
                    "La clave de almacenamiento es obligatoria");
        }

        validarOrden(request.getOrden());
    }

    private void validarOrden(Integer orden) {

        if (orden == null || orden < 1) {
            throw new IllegalArgumentException(
                    "El orden debe ser mayor o igual a 1");
        }
    }
}
