package com.ecommerce.backend.plantilla.service;

import com.ecommerce.backend.exception.PlantillaCodigoDuplicadoException;
import com.ecommerce.backend.exception.PlantillaNoEncontradaException;
import com.ecommerce.backend.plantilla.dto.ActualizarEstadoPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.ActualizarPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.CrearPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.PlantillaResponse;
import com.ecommerce.backend.plantilla.mapper.PlantillaMapper;
import com.ecommerce.backend.plantilla.model.Plantilla;
import com.ecommerce.backend.plantilla.repository.PlantillaRepository;
import com.ecommerce.backend.shared.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlantillaServiceImpl implements PlantillaService {

    private final PlantillaRepository plantillaRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public PlantillaResponse crearPlantilla(CrearPlantillaRequest request) {
        String codigo = request.codigo() != null ? request.codigo().trim() : "";
        String nombre = request.nombre() != null ? request.nombre().trim() : "";

        if (codigo.isEmpty() || nombre.isEmpty()) {
            throw new IllegalArgumentException("El código y nombre de la plantilla no pueden estar vacíos");
        }

        if (plantillaRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new PlantillaCodigoDuplicadoException("Ya existe una plantilla con el código: " + codigo);
        }

        String previewUrl = null;
        if (request.previewUrl() != null && !request.previewUrl().isBlank()) {
            previewUrl = request.previewUrl().trim();
        }

        Plantilla plantilla = Plantilla.builder()
                .codigo(codigo)
                .nombre(nombre)
                .previewUrl(previewUrl)
                .activa(request.activaOrDefault())
                .build();

        try {
            Plantilla guardada = plantillaRepository.saveAndFlush(plantilla);
            return PlantillaMapper.toResponse(guardada);
        } catch (DataIntegrityViolationException e) {
            throw new PlantillaCodigoDuplicadoException("Ya existe una plantilla con el código: " + codigo);
        }
    }

    @Override
    @Transactional
    public PlantillaResponse actualizarPlantilla(Short idPlantilla, ActualizarPlantillaRequest request) {
        Plantilla plantilla = plantillaRepository.findById(idPlantilla)
                .orElseThrow(() -> new PlantillaNoEncontradaException("No se encontró la plantilla con id: " + idPlantilla));

        if (request.nombre() != null) {
            String nombre = request.nombre().trim();
            if (nombre.isEmpty()) {
                throw new IllegalArgumentException("El nombre de la plantilla no puede estar vacío");
            }
            plantilla.setNombre(nombre);
        }

        if (request.previewUrl() != null) {
            String previewUrl = request.previewUrl().isBlank() ? null : request.previewUrl().trim();
            plantilla.setPreviewUrl(previewUrl);
        }

        if (request.activa() != null) {
            plantilla.setActiva(request.activa());
        }

        Plantilla guardada = plantillaRepository.saveAndFlush(plantilla);
        return PlantillaMapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public PlantillaResponse actualizarEstadoPlantilla(Short idPlantilla, ActualizarEstadoPlantillaRequest request) {
        Plantilla plantilla = plantillaRepository.findById(idPlantilla)
                .orElseThrow(() -> new PlantillaNoEncontradaException("No se encontró la plantilla con id: " + idPlantilla));

        plantilla.setActiva(request.activa());

        Plantilla guardada = plantillaRepository.saveAndFlush(plantilla);
        return PlantillaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlantillaResponse> listarPlantillas(Boolean incluirInactivas) {
        boolean esAdmin = currentUserProvider.hasRole("ADMIN_GENERAL");

        List<Plantilla> plantillas;
        if (esAdmin && Boolean.TRUE.equals(incluirInactivas)) {
            plantillas = plantillaRepository.findAllByOrderByIdPlantillaAsc();
        } else {
            plantillas = plantillaRepository.findAllByActivaTrueOrderByIdPlantillaAsc();
        }

        return plantillas.stream()
                .map(PlantillaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlantillaResponse obtenerPorId(Short idPlantilla) {
        Plantilla plantilla = plantillaRepository.findById(idPlantilla)
                .orElseThrow(() -> new PlantillaNoEncontradaException("No se encontró la plantilla con id: " + idPlantilla));

        boolean esAdmin = currentUserProvider.hasRole("ADMIN_GENERAL");
        if (!Boolean.TRUE.equals(plantilla.getActiva()) && !esAdmin) {
            throw new PlantillaNoEncontradaException("No se encontró la plantilla con id: " + idPlantilla);
        }

        return PlantillaMapper.toResponse(plantilla);
    }
}
