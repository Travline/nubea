package com.ecommerce.backend.catalogo.service;

import org.springframework.stereotype.Service;

import com.ecommerce.backend.catalogo.dto.request.ValorVarianteRequest;
import com.ecommerce.backend.catalogo.dto.response.ValorVarianteResponse;
import com.ecommerce.backend.catalogo.mapper.ValorVarianteMapper;
import com.ecommerce.backend.catalogo.model.ValorVariante;
import com.ecommerce.backend.catalogo.repository.ValorVarianteRepository;
import com.ecommerce.backend.catalogo.repository.VarianteRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValorVarianteService {

    private final ValorVarianteRepository valorVarianteRepository;
    private final VarianteRepository varianteRepository;

    public ValorVarianteService(
            ValorVarianteRepository valorVarianteRepository,
            VarianteRepository varianteRepository) {
        this.valorVarianteRepository = valorVarianteRepository;
        this.varianteRepository = varianteRepository;
    }

    @Transactional
    public ValorVarianteResponse crearValorVariante(
            UUID idVariante,
            UUID idTienda,
            ValorVarianteRequest request) {
        if (request == null || request.getValor() == null
                || request.getValor().isBlank()) {
            throw new IllegalArgumentException(
                    "El valor de la variante no puede estar vacío");
        }

        // Verificar que la variante pertenezca a la tienda
        varianteRepository.findByIdVarianteAndIdTienda(idVariante, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La variante no existe en esta tienda"));

        String valor = request.getValor().trim();

        // Verificar si el valor ya existe dentro de la variante
        if (valorVarianteRepository.existsByIdVarianteAndValorIgnoreCase(
                idVariante, valor)) {
            throw new IllegalArgumentException(
                    "Este valor ya existe para la variante");
        }

        ValorVariante nuevoValor = new ValorVariante();
        nuevoValor.setIdVariante(idVariante);
        nuevoValor.setValor(valor);

        ValorVariante guardado = valorVarianteRepository.save(nuevoValor);

        return ValorVarianteMapper.toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<ValorVarianteResponse> listarValores(
            UUID idVariante,
            UUID idTienda) {
        // Verificar que la variante pertenezca a la tienda
        varianteRepository.findByIdVarianteAndIdTienda(idVariante, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La variante no existe en esta tienda"));

        // Obtener los valores de la variante
        return valorVarianteRepository.findByIdVariante(idVariante)
                .stream()
                .map(ValorVarianteMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ValorVarianteResponse obtenerValorPorId(
            UUID idValor,
            UUID idVariante,
            UUID idTienda) {
        // Verificar que la variante pertenezca a la tienda
        varianteRepository.findByIdVarianteAndIdTienda(idVariante, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La variante no existe en esta tienda"));

        // Buscar el valor dentro de esa variante
        ValorVariante valor = valorVarianteRepository
                .findByIdValorAndIdVariante(idValor, idVariante)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El valor no existe en esta variante"));

        return ValorVarianteMapper.toResponse(valor);
    }

    @Transactional
    public ValorVarianteResponse actualizarValor(
            UUID idValor,
            UUID idVariante,
            UUID idTienda,
            ValorVarianteRequest request) {
        // Validar la solicitud
        if (request == null || request.getValor() == null
                || request.getValor().isBlank()) {
            throw new IllegalArgumentException(
                    "El valor de la variante no puede estar vacío");
        }

        // Verificar que la variante pertenezca a la tienda
        varianteRepository.findByIdVarianteAndIdTienda(idVariante, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La variante no existe en esta tienda"));

        // Buscar el valor dentro de la variante
        ValorVariante valorExistente = valorVarianteRepository
                .findByIdValorAndIdVariante(idValor, idVariante)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El valor no existe en esta variante"));

        String nuevoValor = request.getValor().trim();

        // Comprobar que no exista otro valor con el mismo nombre
        if (!valorExistente.getValor().equalsIgnoreCase(nuevoValor)
                && valorVarianteRepository.existsByIdVarianteAndValorIgnoreCase(
                        idVariante, nuevoValor)) {
            throw new IllegalArgumentException(
                    "Ya existe otro valor con ese nombre para esta variante");
        }

        valorExistente.setValor(nuevoValor);

        ValorVariante actualizado = valorVarianteRepository.save(valorExistente);

        return ValorVarianteMapper.toResponse(actualizado);
    }

}