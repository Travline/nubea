package com.ecommerce.backend.plantilla.service;

import com.ecommerce.backend.exception.PlantillaCodigoDuplicadoException;
import com.ecommerce.backend.plantilla.dto.CrearPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.PlantillaResponse;
import com.ecommerce.backend.plantilla.mapper.PlantillaMapper;
import com.ecommerce.backend.plantilla.model.Plantilla;
import com.ecommerce.backend.plantilla.repository.PlantillaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlantillaServiceImpl implements PlantillaService {

    private final PlantillaRepository plantillaRepository;

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
}
