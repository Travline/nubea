package com.nubea.spring.modules.categoria.services;

import com.nubea.spring.modules.categoria.dto.CategoriaRequest;
import com.nubea.spring.modules.categoria.dto.CategoriaResponse;
import com.nubea.spring.modules.categoria.entities.Categoria;
import com.nubea.spring.modules.categoria.repositories.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    // Listar todas las categorías
    public List<CategoriaResponse> listarTodas() {
        return categoriaRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    // Buscar categoría por ID
    public Optional<CategoriaResponse> buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .map(this::convertirAResponse);
    }

    // Crear categoría
    public CategoriaResponse crear(CategoriaRequest request) {

        Categoria categoria = new Categoria();

        categoria.setIdTienda(request.getIdTienda());
        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());

        if (request.getActivo() != null) {
            categoria.setActivo(request.getActivo());
        }

        Categoria categoriaGuardada = categoriaRepository.save(categoria);

        return convertirAResponse(categoriaGuardada);
    }

    // Actualizar categoría
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {

        return categoriaRepository.findById(id)
                .map(categoria -> {

                    categoria.setIdTienda(request.getIdTienda());
                    categoria.setNombre(request.getNombre());
                    categoria.setDescripcion(request.getDescripcion());

                    if (request.getActivo() != null) {
                        categoria.setActivo(request.getActivo());
                    }

                    Categoria categoriaActualizada =
                            categoriaRepository.save(categoria);

                    return convertirAResponse(categoriaActualizada);
                })
                .orElseThrow(() -> new RuntimeException(
                        "Categoría no encontrada con ID: " + id
                ));
    }

    // Eliminar categoría
    public void eliminar(Long id) {

        if (!categoriaRepository.existsById(id)) {
            throw new RuntimeException(
                    "Categoría no encontrada con ID: " + id
            );
        }

        categoriaRepository.deleteById(id);
    }

    // Convertir Entity -> Response
    private CategoriaResponse convertirAResponse(Categoria categoria) {

        CategoriaResponse response = new CategoriaResponse();

        response.setIdCategoria(categoria.getIdCategoria());
        response.setIdTienda(categoria.getIdTienda());
        response.setNombre(categoria.getNombre());
        response.setDescripcion(categoria.getDescripcion());
        response.setActivo(categoria.getActivo());

        return response;
    }
}