
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.dto.request.CategoriaRequest;
import com.ecommerce.backend.catalogo.dto.response.CategoriaResponse;
import com.ecommerce.backend.catalogo.mapper.CategoriaMapper;
import com.ecommerce.backend.catalogo.model.Categoria;
import com.ecommerce.backend.catalogo.repository.CategoriaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(
            CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public CategoriaResponse crearCategoria(
            UUID idTienda,
            CategoriaRequest request) {

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

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarCategorias(
            UUID idTienda) {

        return categoriaRepository.findByIdTienda(idTienda)
                .stream()
                .map(CategoriaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtenerCategoriaPorId(
            UUID idCategoria,
            UUID idTienda) {

        Categoria categoria = buscarCategoria(
                idCategoria, idTienda);

        return CategoriaMapper.toResponse(categoria);
    }

    @Transactional
    public CategoriaResponse actualizarCategoria(
            UUID idCategoria,
            UUID idTienda,
            CategoriaRequest request) {

        validarSolicitud(request);

        Categoria categoria = buscarCategoria(
                idCategoria, idTienda);

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

    @Transactional
    public CategoriaResponse cambiarEstadoCategoria(
            UUID idCategoria,
            UUID idTienda,
            Boolean activa) {

        if (activa == null) {
            throw new IllegalArgumentException(
                    "El estado de la categoría es obligatorio");
        }

        Categoria categoria = buscarCategoria(
                idCategoria, idTienda);

        categoria.setActiva(activa);

        Categoria actualizada = categoriaRepository.save(categoria);

        return CategoriaMapper.toResponse(actualizada);
    }

    private Categoria buscarCategoria(
            UUID idCategoria,
            UUID idTienda) {

        return categoriaRepository
                .findByIdCategoriaAndIdTienda(
                        idCategoria, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La categoría no existe en esta tienda"));
    }

    private void validarSlugDisponible(
            UUID idTienda,
            String slug) {

        if (categoriaRepository.existsByIdTiendaAndSlug(
                idTienda, slug)) {

            throw new IllegalArgumentException(
                    "Ya existe una categoría con ese slug en la tienda");
        }
    }

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
