
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.dto.request.VarianteRequest;
import com.ecommerce.backend.catalogo.dto.response.VarianteResponse;
import com.ecommerce.backend.catalogo.mapper.VarianteMapper;
import com.ecommerce.backend.catalogo.model.Variante;
import com.ecommerce.backend.catalogo.repository.VarianteRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VarianteService {

    private final VarianteRepository varianteRepository;

    public VarianteService(
            VarianteRepository varianteRepository) {
        this.varianteRepository = varianteRepository;
    }

    @Transactional
    public VarianteResponse crearVariante(
            UUID idTienda,
            VarianteRequest request) {

        String nombre = validarNombre(request);

        if (varianteRepository.existsByIdTiendaAndNombreIgnoreCase(
                idTienda, nombre)) {

            throw new IllegalArgumentException(
                    "Ya existe una variante con ese nombre en la tienda");
        }

        Variante variante = new Variante();

        variante.setIdTienda(idTienda);
        variante.setNombre(nombre);

        Variante guardada = varianteRepository.save(variante);

        return VarianteMapper.toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<VarianteResponse> listarVariantes(UUID idTienda) {

        return varianteRepository.findByIdTienda(idTienda)
                .stream()
                .map(VarianteMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VarianteResponse obtenerVariantePorId(
            UUID idVariante,
            UUID idTienda) {

        Variante variante = buscarVariante(idVariante, idTienda);

        return VarianteMapper.toResponse(variante);
    }

    @Transactional
    public VarianteResponse actualizarVariante(
            UUID idVariante,
            UUID idTienda,
            VarianteRequest request) {

        Variante variante = buscarVariante(idVariante, idTienda);

        String nombre = validarNombre(request);

        if (!variante.getNombre().equalsIgnoreCase(nombre)
                && varianteRepository.existsByIdTiendaAndNombreIgnoreCase(
                        idTienda, nombre)) {

            throw new IllegalArgumentException(
                    "Ya existe una variante con ese nombre en la tienda");
        }

        variante.setNombre(nombre);

        Variante actualizada = varianteRepository.save(variante);

        return VarianteMapper.toResponse(actualizada);
    }

    private Variante buscarVariante(
            UUID idVariante,
            UUID idTienda) {

        return varianteRepository
                .findByIdVarianteAndIdTienda(idVariante, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La variante no existe en esta tienda"));
    }

    private String validarNombre(VarianteRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud de variante no puede estar vacía");
        }

        if (request.getNombre() == null
                || request.getNombre().isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre de la variante es obligatorio");
        }

        String nombre = request.getNombre().trim();

        // Ajustar este límite al definido en el SQL maestro.
        if (nombre.length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre de la variante no puede superar los 100 caracteres");
        }

        return nombre;
    }
}
