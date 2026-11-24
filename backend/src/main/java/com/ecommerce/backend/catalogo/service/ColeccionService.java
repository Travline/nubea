
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.dto.request.ColeccionRequest;
import com.ecommerce.backend.catalogo.dto.response.ColeccionResponse;
import com.ecommerce.backend.catalogo.mapper.ColeccionMapper;
import com.ecommerce.backend.catalogo.model.Coleccion;
import com.ecommerce.backend.catalogo.repository.ColeccionRepository;

import java.util.List;
import java.util.UUID;

import com.ecommerce.backend.shared.exception.MensajesUsuario;
import com.ecommerce.backend.shared.exception.RecursosDuplicadosException;
import com.ecommerce.backend.shared.exception.ValidacionCampoException;
import com.ecommerce.backend.shared.utils.SlugUtil;

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
        String slugRecibido = slugOpcional(request);
        String slug;
        if (slugRecibido == null) {
            slug = generarSlugUnico(idTienda, nombre);
        } else {
            validarFormatoSlug(slugRecibido);
            validarSlugDisponible(idTienda, slugRecibido);
            slug = slugRecibido;
        }

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
        // Cambiar el nombre no cambia el slug: los enlaces deben ser estables.
        String slugRecibido = slugOpcional(request);
        String slug = slugRecibido == null ? coleccion.getSlug() : slugRecibido;

        // Validar el slug únicamente si ha cambiado.
        if (!coleccion.getSlug().equals(slug)) {
            validarFormatoSlug(slug);
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

            throw new RecursosDuplicadosException(
                    "slug", MensajesUsuario.COLECCION_DIRECCION_DUPLICADA);
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

        if (request.getSlug() != null && request.getSlug().trim().length() > LARGO_SLUG) {
            throw new ValidacionCampoException("slug",
                    "La dirección web no puede superar los " + LARGO_SLUG + " caracteres");
        }
    }

    private static final int LARGO_SLUG = 150; // coleccion.slug VARCHAR(150)

    private static String slugOpcional(ColeccionRequest request) {
        return request.getSlug() == null || request.getSlug().isBlank() ? null : request.getSlug().trim();
    }

    private static void validarFormatoSlug(String slug) {
        if (!SlugUtil.esValido(slug)) {
            throw new ValidacionCampoException("slug", MensajesUsuario.DIRECCION_FORMATO);
        }
    }

    // Igual que con la tienda: base, base-2, base-3...
    private String generarSlugUnico(UUID idTienda, String nombre) {
        String base = SlugUtil.generar(nombre, "coleccion", LARGO_SLUG - 6);
        String slug = base;
        int i = 2;
        while (coleccionRepository.existsByIdTiendaAndSlug(idTienda, slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
