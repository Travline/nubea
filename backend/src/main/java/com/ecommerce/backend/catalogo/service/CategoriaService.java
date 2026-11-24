
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.dto.request.CategoriaRequest;
import com.ecommerce.backend.catalogo.dto.response.CategoriaResponse;
import com.ecommerce.backend.catalogo.mapper.CategoriaMapper;
import com.ecommerce.backend.catalogo.model.Categoria;
import com.ecommerce.backend.catalogo.repository.CategoriaRepository;
import com.ecommerce.backend.tienda.repository.TiendaRepository;

import com.ecommerce.backend.shared.exception.MensajesUsuario;
import com.ecommerce.backend.shared.exception.RecursosDuplicadosException;
import com.ecommerce.backend.shared.exception.ValidacionCampoException;
import com.ecommerce.backend.shared.utils.SlugUtil;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TiendaRepository tiendaRepository;

    public CategoriaService(
            CategoriaRepository categoriaRepository,
            TiendaRepository tiendaRepository) {

        this.categoriaRepository = categoriaRepository;
        this.tiendaRepository = tiendaRepository;
    }

    // Crear categoría
    @Transactional
    public CategoriaResponse crearCategoria(
            UUID idTienda,
            CategoriaRequest request) {

        validarTienda(idTienda);
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

        Categoria categoria = new Categoria();

        categoria.setIdTienda(idTienda);
        categoria.setNombre(nombre);
        categoria.setSlug(slug);
        categoria.setActiva(true);

        Categoria guardada = categoriaRepository.save(categoria);

        return CategoriaMapper.toResponse(guardada);
    }

    // Listar categorías de una tienda
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarCategorias(UUID idTienda) {

        validarTienda(idTienda);

        return categoriaRepository.findByIdTienda(idTienda)
                .stream()
                .map(CategoriaMapper::toResponse)
                .toList();
    }

    // Obtener categoría por ID
    @Transactional(readOnly = true)
    public CategoriaResponse obtenerCategoriaPorId(
            UUID idCategoria,
            UUID idTienda) {

        validarTienda(idTienda);

        Categoria categoria = buscarCategoria(idCategoria, idTienda);

        return CategoriaMapper.toResponse(categoria);
    }

    // Actualizar categoría
    @Transactional
    public CategoriaResponse actualizarCategoria(
            UUID idCategoria,
            UUID idTienda,
            CategoriaRequest request) {

        validarTienda(idTienda);
        validarSolicitud(request);

        Categoria categoria = buscarCategoria(idCategoria, idTienda);

        String nombre = request.getNombre().trim();
        // Cambiar el nombre no cambia el slug: los enlaces deben ser estables.
        String slugRecibido = slugOpcional(request);
        String slug = slugRecibido == null ? categoria.getSlug() : slugRecibido;

        if (!categoria.getSlug().equals(slug)) {
            validarFormatoSlug(slug);
            validarSlugDisponible(idTienda, slug);
        }

        categoria.setNombre(nombre);
        categoria.setSlug(slug);

        Categoria actualizada = categoriaRepository.save(categoria);

        return CategoriaMapper.toResponse(actualizada);
    }

    // Cambiar estado de categoría
    @Transactional
    public CategoriaResponse cambiarEstadoCategoria(
            UUID idCategoria,
            UUID idTienda,
            Boolean activa) {

        validarTienda(idTienda);

        if (activa == null) {
            throw new IllegalArgumentException(
                    "El estado de la categoría es obligatorio");
        }

        Categoria categoria = buscarCategoria(idCategoria, idTienda);

        categoria.setActiva(activa);

        Categoria actualizada = categoriaRepository.save(categoria);

        return CategoriaMapper.toResponse(actualizada);
    }

    // Validar existencia de tienda
    private void validarTienda(UUID idTienda) {

        if (idTienda == null) {
            throw new IllegalArgumentException(
                    "La tienda es obligatoria");
        }

        tiendaRepository.findById(idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La tienda no existe"));
    }

    // Buscar categoría dentro de una tienda
    private Categoria buscarCategoria(
            UUID idCategoria,
            UUID idTienda) {

        return categoriaRepository
                .findByIdCategoriaAndIdTienda(idCategoria, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La categoría no existe en esta tienda"));
    }

    // Validar disponibilidad del slug
    private void validarSlugDisponible(
            UUID idTienda,
            String slug) {

        if (categoriaRepository.existsByIdTiendaAndSlug(idTienda, slug)) {
            throw new RecursosDuplicadosException(
                    "slug", MensajesUsuario.CATEGORIA_DIRECCION_DUPLICADA);
        }
    }

    // Validar solicitud
    private void validarSolicitud(CategoriaRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud de categoría no puede estar vacía");
        }

        if (request.getNombre() == null
                || request.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la categoría es obligatorio");
        }

        if (request.getNombre().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar los 100 caracteres");
        }

        if (request.getSlug() != null && request.getSlug().trim().length() > LARGO_SLUG) {
            throw new ValidacionCampoException("slug",
                    "La dirección web no puede superar los " + LARGO_SLUG + " caracteres");
        }
    }

    private static final int LARGO_SLUG = 120; // categoria.slug VARCHAR(120)

    private static String slugOpcional(CategoriaRequest request) {
        return request.getSlug() == null || request.getSlug().isBlank() ? null : request.getSlug().trim();
    }

    private static void validarFormatoSlug(String slug) {
        if (!SlugUtil.esValido(slug)) {
            throw new ValidacionCampoException("slug", MensajesUsuario.DIRECCION_FORMATO);
        }
    }

    // Igual que con la tienda: base, base-2, base-3...
    private String generarSlugUnico(UUID idTienda, String nombre) {
        String base = SlugUtil.generar(nombre, "categoria", LARGO_SLUG - 6);
        String slug = base;
        int i = 2;
        while (categoriaRepository.existsByIdTiendaAndSlug(idTienda, slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
