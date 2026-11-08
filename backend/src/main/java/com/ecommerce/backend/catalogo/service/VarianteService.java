
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.dto.request.VarianteRequest;
import com.ecommerce.backend.catalogo.dto.response.VarianteResponse;
import com.ecommerce.backend.catalogo.mapper.VarianteMapper;
import com.ecommerce.backend.catalogo.model.Variante;
import com.ecommerce.backend.catalogo.repository.VarianteRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Service
public class VarianteService {

    private final VarianteRepository varianteRepository;

    public VarianteService(VarianteRepository varianteRepository) {
        this.varianteRepository = varianteRepository;
    }

    @Transactional
    public VarianteResponse crearVariante(
            UUID idTienda,
            VarianteRequest request) {

        if (request.getNombre() == null
                || request.getNombre().isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre de la variante es obligatorio");
        }

        String nombre = request.getNombre().trim();

        if (varianteRepository.existsByIdTiendaAndNombreIgnoreCase(
                idTienda, nombre)) {

            throw new IllegalArgumentException(
                    "Ya existe una variante con ese nombre en la tienda");
        }

        Variante variante = new Variante();

        variante.setIdTienda(idTienda);
        variante.setNombre(nombre);

        Variante varianteGuardada =
                varianteRepository.save(variante);

        return VarianteMapper.toResponse(varianteGuardada);
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

    Variante variante = varianteRepository
            .findByIdVarianteAndIdTienda(idVariante, idTienda)
            .orElseThrow(() -> new IllegalArgumentException(
                    "La variante no existe en esta tienda"));

    return VarianteMapper.toResponse(variante);
}




@Transactional
public VarianteResponse actualizarVariante(
        UUID idVariante,
        UUID idTienda,
        VarianteRequest request) {

    Variante variante = varianteRepository
            .findByIdVarianteAndIdTienda(idVariante, idTienda)
            .orElseThrow(() -> new IllegalArgumentException(
                    "La variante no existe en esta tienda"));

    if (request.getNombre() == null
            || request.getNombre().isBlank()) {

        throw new IllegalArgumentException(
                "El nombre de la variante es obligatorio");
    }

    String nombre = request.getNombre().trim();

    if (!variante.getNombre().equalsIgnoreCase(nombre)
            && varianteRepository.existsByIdTiendaAndNombreIgnoreCase(
                    idTienda, nombre)) {

        throw new IllegalArgumentException(
                "Ya existe una variante con ese nombre en la tienda");
    }

    variante.setNombre(nombre);

    Variante varianteActualizada =
            varianteRepository.save(variante);

    return VarianteMapper.toResponse(varianteActualizada);
}




}
