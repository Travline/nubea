
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.dto.request.CategoriaRequest;
import com.ecommerce.backend.catalogo.dto.response.CategoriaResponse;
import com.ecommerce.backend.catalogo.mapper.CategoriaMapper;
import com.ecommerce.backend.catalogo.model.Categoria;
import com.ecommerce.backend.catalogo.repository.CategoriaRepository;
import com.ecommerce.backend.tienda.repository.TiendaRepository;

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
        String slug = request.getSlug().trim();

        validarSlugDisponible(idTienda, slug);

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
        String slug = request.getSlug().trim();

        if (!categoria.getSlug().equals(slug)) {
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
            throw new IllegalArgumentException(
                    "Ya existe una categoría con ese slug en la tienda");
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

        if (request.getSlug() == null
                || request.getSlug().isBlank()) {
            throw new IllegalArgumentException(
                    "El slug de la categoría es obligatorio");
        }

        if (request.getSlug().trim().length() > 150) {
            throw new IllegalArgumentException(
                    "El slug no puede superar los 150 caracteres");
        }
    }
}
