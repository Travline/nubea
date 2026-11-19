
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.model.ImagenProducto;
import com.ecommerce.backend.catalogo.model.PresentacionImagen;
import com.ecommerce.backend.catalogo.model.PresentacionImagenId;
import com.ecommerce.backend.catalogo.model.PresentacionProducto;

import com.ecommerce.backend.catalogo.repository.ImagenProductoRepository;
import com.ecommerce.backend.catalogo.repository.PresentacionImagenRepository;
import com.ecommerce.backend.catalogo.repository.PresentacionProductoRepository;
import com.ecommerce.backend.catalogo.repository.ProductoRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresentacionImagenService {

    private final PresentacionImagenRepository presentacionImagenRepository;
    private final PresentacionProductoRepository presentacionProductoRepository;
    private final ImagenProductoRepository imagenProductoRepository;
    private final ProductoRepository productoRepository;

    public PresentacionImagenService(
            PresentacionImagenRepository presentacionImagenRepository,
            PresentacionProductoRepository presentacionProductoRepository,
            ImagenProductoRepository imagenProductoRepository,
            ProductoRepository productoRepository) {

        this.presentacionImagenRepository = presentacionImagenRepository;
        this.presentacionProductoRepository = presentacionProductoRepository;
        this.imagenProductoRepository = imagenProductoRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public void asociarImagen(
            UUID idPresentacion,
            UUID idImagen,
            UUID idTienda) {

        // Buscar la presentación.
        PresentacionProducto presentacion =
                presentacionProductoRepository
                        .findByIdPresentacionAndIdTienda(
                                idPresentacion, idTienda)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "La presentación no existe en esta tienda"));

        // Buscar la imagen y obtener su producto.
        ImagenProducto imagen = imagenProductoRepository
                .findById(idImagen)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La imagen no existe"));

        // Comprobar que la imagen pertenezca a la misma tienda.
        productoRepository.findByIdProductoAndIdTienda(
                imagen.getIdProducto(), idTienda
        ).orElseThrow(() -> new IllegalArgumentException(
                "La imagen no pertenece a esta tienda"));

        // Comprobar que ambos pertenezcan al mismo producto.
        if (!presentacion.getIdProducto().equals(
                imagen.getIdProducto())) {
            throw new IllegalArgumentException(
                    "La imagen y la presentación pertenecen a productos diferentes");
        }

        // Evitar asociaciones duplicadas.
        if (presentacionImagenRepository
                .existsById_IdPresentacionAndId_IdImagen(
                        idPresentacion, idImagen)) {
            throw new IllegalArgumentException(
                    "La imagen ya está asociada a esta presentación");
        }

        PresentacionImagen relacion = new PresentacionImagen();

        relacion.setId(
                new PresentacionImagenId(idPresentacion, idImagen));

        presentacionImagenRepository.save(relacion);
    }

    @Transactional(readOnly = true)
    public List<ImagenProducto> listarImagenesPorPresentacion(
            UUID idPresentacion,
            UUID idTienda) {

        validarPresentacion(idPresentacion, idTienda);

        return presentacionImagenRepository
                .findById_IdPresentacion(idPresentacion)
                .stream()
                .map(relacion -> imagenProductoRepository
                        .findById(relacion.getId().getIdImagen())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No se encontró una imagen asociada")))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PresentacionProducto> listarPresentacionesPorImagen(
            UUID idImagen,
            UUID idTienda) {

        ImagenProducto imagen = buscarImagenDeTienda(
                idImagen, idTienda);

        return presentacionImagenRepository
                .findById_IdImagen(idImagen)
                .stream()
                .map(relacion -> presentacionProductoRepository
                        .findByIdPresentacionAndIdProducto(
                                relacion.getId().getIdPresentacion(),
                                imagen.getIdProducto())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No se encontró una presentación asociada")))
                .toList();
    }

    @Transactional
    public void desasociarImagen(
            UUID idPresentacion,
            UUID idImagen,
            UUID idTienda) {

        validarPresentacion(idPresentacion, idTienda);

        buscarImagenDeTienda(idImagen, idTienda);

        PresentacionImagen relacion =
                presentacionImagenRepository.findById(
                        new PresentacionImagenId(
                                idPresentacion, idImagen))
                        .orElseThrow(() -> new IllegalArgumentException(
                                "La imagen no está asociada a esta presentación"));

        presentacionImagenRepository.delete(relacion);
    }

    private PresentacionProducto validarPresentacion(
            UUID idPresentacion,
            UUID idTienda) {

        return presentacionProductoRepository
                .findByIdPresentacionAndIdTienda(
                        idPresentacion, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La presentación no existe en esta tienda"));
    }

    private ImagenProducto buscarImagenDeTienda(
            UUID idImagen,
            UUID idTienda) {

        ImagenProducto imagen = imagenProductoRepository
                .findById(idImagen)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La imagen no existe"));

        productoRepository.findByIdProductoAndIdTienda(
                imagen.getIdProducto(), idTienda
        ).orElseThrow(() -> new IllegalArgumentException(
                "La imagen no pertenece a esta tienda"));

        return imagen;
    }
}
