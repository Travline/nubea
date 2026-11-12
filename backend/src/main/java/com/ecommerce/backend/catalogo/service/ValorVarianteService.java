
package com.ecommerce.backend.catalogo.service;

import com.ecommerce.backend.catalogo.dto.request.ValorVarianteRequest;
import com.ecommerce.backend.catalogo.dto.response.ValorVarianteResponse;
import com.ecommerce.backend.catalogo.mapper.ValorVarianteMapper;
import com.ecommerce.backend.catalogo.model.ValorVariante;
import com.ecommerce.backend.catalogo.repository.ValorVarianteRepository;
import com.ecommerce.backend.catalogo.repository.VarianteRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
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

        String valor = validarValor(request);

        validarVariante(idVariante, idTienda);

        validarValorDuplicado(idVariante, valor);

        ValorVariante nuevoValor = new ValorVariante();

        nuevoValor.setIdVariante(idVariante);
        nuevoValor.setValor(valor);

        ValorVariante guardado =
                valorVarianteRepository.save(nuevoValor);

        return ValorVarianteMapper.toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<ValorVarianteResponse> listarValores(
            UUID idVariante,
            UUID idTienda) {

        validarVariante(idVariante, idTienda);

        return valorVarianteRepository
                .findByIdVariante(idVariante)
                .stream()
                .map(ValorVarianteMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ValorVarianteResponse obtenerValorPorId(
            UUID idValor,
            UUID idVariante,
            UUID idTienda) {

        validarVariante(idVariante, idTienda);

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

        String nuevoValor = validarValor(request);

        validarVariante(idVariante, idTienda);

        ValorVariante valorExistente = valorVarianteRepository
                .findByIdValorAndIdVariante(idValor, idVariante)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El valor no existe en esta variante"));

        if (!valorExistente.getValor().equalsIgnoreCase(nuevoValor)) {
            validarValorDuplicado(idVariante, nuevoValor);
        }

        valorExistente.setValor(nuevoValor);

        ValorVariante actualizado =
                valorVarianteRepository.save(valorExistente);

        return ValorVarianteMapper.toResponse(actualizado);
    }

    private void validarVariante(
            UUID idVariante,
            UUID idTienda) {

        varianteRepository
                .findByIdVarianteAndIdTienda(idVariante, idTienda)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La variante no existe en esta tienda"));
    }

    private void validarValorDuplicado(
            UUID idVariante,
            String valor) {

        if (valorVarianteRepository
                .existsByIdVarianteAndValorIgnoreCase(
                        idVariante, valor)) {

            throw new IllegalArgumentException(
                    "Este valor ya existe para la variante");
        }
    }

    private String validarValor(ValorVarianteRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud del valor no puede estar vacía");
        }

        if (request.getValor() == null
                || request.getValor().isBlank()) {

            throw new IllegalArgumentException(
                    "El valor de la variante es obligatorio");
        }

        String valor = request.getValor().trim();

        // Límite provisional: confirmar con el SQL maestro.
        if (valor.length() > 100) {
            throw new IllegalArgumentException(
                    "El valor no puede superar los 100 caracteres");
        }

        return valor;
    }
}
