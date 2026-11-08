
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.dto.request.CategoriaRequest;
import com.ecommerce.backend.catalogo.dto.response.CategoriaResponse;
import com.ecommerce.backend.catalogo.mapper.CategoriaMapper;
import com.ecommerce.backend.catalogo.model.Categoria;
import com.ecommerce.backend.catalogo.repository.CategoriaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public CategoriaResponse crearCategoria(
            UUID idTienda,
            CategoriaRequest request) {

        if (categoriaRepository.existsByIdTiendaAndSlug(
                idTienda, request.getSlug())) {

            throw new IllegalArgumentException(
                    "Ya existe una categoría con ese slug en la tienda");
        }

        Categoria categoria = new Categoria();

        categoria.setIdTienda(idTienda);
        categoria.setNombre(request.getNombre());
        categoria.setSlug(request.getSlug());
        categoria.setActiva(true);

        Categoria categoriaGuardada =
                categoriaRepository.save(categoria);

        return CategoriaMapper.toResponse(categoriaGuardada);
    }

    
@Transactional(readOnly = true)
public List<CategoriaResponse> listarCategorias(UUID idTienda) {

    return categoriaRepository.findByIdTienda(idTienda)
            .stream()
            .map(CategoriaMapper::toResponse)
            .toList();
}


@Transactional(readOnly = true)
public CategoriaResponse obtenerCategoriaPorId(
        UUID idCategoria,
        UUID idTienda) {

    Categoria categoria = categoriaRepository
            .findByIdCategoriaAndIdTienda(idCategoria, idTienda)
            .orElseThrow(() -> new IllegalArgumentException(
                    "La categoría no existe en esta tienda"));

    return CategoriaMapper.toResponse(categoria);
}


@Transactional
public CategoriaResponse actualizarCategoria(
        UUID idCategoria,
        UUID idTienda,
        CategoriaRequest request) {

    Categoria categoria = categoriaRepository
            .findByIdCategoriaAndIdTienda(idCategoria, idTienda)
            .orElseThrow(() -> new IllegalArgumentException(
                    "La categoría no existe en esta tienda"));

    if (!categoria.getSlug().equals(request.getSlug())
            && categoriaRepository.existsByIdTiendaAndSlug(
                    idTienda, request.getSlug())) {

        throw new IllegalArgumentException(
                "Ya existe una categoría con ese slug en la tienda");
    }

    categoria.setNombre(request.getNombre());
    categoria.setSlug(request.getSlug());

    Categoria categoriaActualizada =
            categoriaRepository.save(categoria);

    return CategoriaMapper.toResponse(categoriaActualizada);
}



@Transactional
public CategoriaResponse cambiarEstadoCategoria(
        UUID idCategoria,
        UUID idTienda,
        Boolean activa) {

    Categoria categoria = categoriaRepository
            .findByIdCategoriaAndIdTienda(idCategoria, idTienda)
            .orElseThrow(() -> new IllegalArgumentException(
                    "La categoría no existe en esta tienda"));

    if (activa == null) {
        throw new IllegalArgumentException(
                "El estado de la categoría es obligatorio");
    }

    categoria.setActiva(activa);

    Categoria categoriaActualizada =
            categoriaRepository.save(categoria);

    return CategoriaMapper.toResponse(categoriaActualizada);
}


}
