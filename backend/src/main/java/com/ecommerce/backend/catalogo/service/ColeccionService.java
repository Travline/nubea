
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.dto.request.ColeccionRequest;
import com.ecommerce.backend.catalogo.dto.response.ColeccionResponse;
import com.ecommerce.backend.catalogo.mapper.ColeccionMapper;
import com.ecommerce.backend.catalogo.model.Coleccion;
import com.ecommerce.backend.catalogo.repository.ColeccionRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColeccionService {

    private final ColeccionRepository coleccionRepository;

    public ColeccionService(
            ColeccionRepository coleccionRepository) {
        this.coleccionRepository = coleccionRepository;
    }

    @Transactional
    public ColeccionResponse crearColeccion(
            UUID idTienda,
            ColeccionRequest request) {

        validarSolicitud(request);

        String nombre = request.getNombre().trim();
        String slug = request.getSlug().trim();

        validarSlugDisponible(idTienda, slug);

        Coleccion coleccion = new Coleccion();

        coleccion.setIdTienda(idTienda);
        coleccion.setNombre(nombre);
        coleccion.setSlug(slug);
        coleccion.setActiva(true);

        Coleccion guardada = coleccionRepository.save(coleccion);

        return ColeccionMapper.toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<ColeccionResponse> listarColecciones(
            UUID idTienda) {

        return coleccionRepository.findByIdTienda(idTienda)
                .stream()
                .map(ColeccionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ColeccionResponse> listarColeccionesActivas(
            UUID idTienda) {

        return coleccionRepository
                .findByIdTiendaAndActivaTrue(idTienda)
                .stream()
                .map(ColeccionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ColeccionResponse obtenerColeccionPorId(
            UUID idColeccion,
            UUID idTienda) {

        Coleccion coleccion = buscarColeccion(
                idColeccion, idTienda);

        return ColeccionMapper.toResponse(coleccion);
    }

    @Transactional
    public ColeccionResponse actualizarColeccion(
            UUID idColeccion,
            UUID idTienda,
            ColeccionRequest request) {

        validarSolicitud(request);

        Coleccion coleccion = buscarColeccion(
                idColeccion, idTienda);

        String nombre = request.getNombre().trim();
        String slug = request.getSlug().trim();

        // Validar el slug únicamente si ha cambiado.
        if (!coleccion.getSlug().equals(slug)) {
            validarSlugDisponible(idTienda, slug);
        }

        coleccion.setNombre(nombre);
        coleccion.setSlug(slug);

        Coleccion actualizada = coleccionRepository.save(coleccion);

        return ColeccionMapper.toResponse(actualizada);
    }

    @Transactional
    public ColeccionResponse cambiarEstadoColeccion(
            UUID idColeccion,
            UUID idTienda,
            Boolean activa) {

        if (activa == null) {
            throw new IllegalArgumentException(
                    "El estado de la colección es obligatorio");
        }

        Coleccion coleccion = buscarColeccion(
                idColeccion, idTienda);

        coleccion.setActiva(activa);

        Coleccion actualizada = coleccionRepository.save(coleccion);

        return ColeccionMapper.toResponse(actualizada);
    }

    // Buscar una colección dentro de una tienda.
    private Coleccion buscarColeccion(
            UUID idColeccion,
            UUID idTienda) {

        return coleccionRepository
                .findByIdColeccionAndIdTienda(
                        idColeccion, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La colección no existe en esta tienda"));
    }

    // Validar que el slug no esté registrado en la tienda.
    private void validarSlugDisponible(
            UUID idTienda,
            String slug) {

        if (coleccionRepository.existsByIdTiendaAndSlug(
                idTienda, slug)) {

            throw new IllegalArgumentException(
                    "Ya existe una colección con ese slug en esta tienda");
        }
    }

    // Validar los datos recibidos.
    private void validarSolicitud(ColeccionRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud de colección no puede estar vacía");
        }

        if (request.getNombre() == null
                || request.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la colección es obligatorio");
        }

        if (request.getNombre().trim().length() > 150) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar los 150 caracteres");
        }

        if (request.getSlug() == null
                || request.getSlug().isBlank()) {
            throw new IllegalArgumentException(
                    "El slug de la colección es obligatorio");
        }

        if (request.getSlug().trim().length() > 150) {
            throw new IllegalArgumentException(
                    "El slug no puede superar los 150 caracteres");
        }

        if (!request.getSlug().trim()
                .matches("^[a-z0-9]+(-[a-z0-9]+)*$")) {

            throw new IllegalArgumentException(
                    "El slug solo puede contener letras minúsculas, "
                            + "números y guiones entre palabras");
        }
    }
}
