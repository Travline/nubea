package com.ecommerce.backend.apariencia.service;

import com.ecommerce.backend.apariencia.dto.ActualizarConfiguracionRequest;
import com.ecommerce.backend.apariencia.dto.ConfiguracionTiendaResponse;
import com.ecommerce.backend.apariencia.mapper.ConfiguracionTiendaMapper;
import com.ecommerce.backend.apariencia.model.ConfiguracionTienda;
import com.ecommerce.backend.apariencia.repository.ConfiguracionTiendaRepository;
import com.ecommerce.backend.exception.ConfiguracionTiendaNoEncontradaException;
import com.ecommerce.backend.exception.PlantillaInactivaException;
import com.ecommerce.backend.exception.PlantillaNoEncontradaException;
import com.ecommerce.backend.plantilla.model.Plantilla;
import com.ecommerce.backend.plantilla.repository.PlantillaRepository;
import com.ecommerce.backend.shared.tienda.TiendaInfo;
import com.ecommerce.backend.shared.tienda.TiendaPort;
import com.ecommerce.backend.shared.tienda.policy.TiendaStatePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfiguracionTiendaServiceImpl implements ConfiguracionTiendaService {

    private final ConfiguracionTiendaRepository configuracionTiendaRepository;
    private final PlantillaRepository plantillaRepository;
    private final TiendaPort tiendaPort;
    private final TiendaStatePolicy tiendaStatePolicy;

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionTiendaResponse obtenerConfiguracion(UUID idTienda) {
        ConfiguracionTienda configuracion = configuracionTiendaRepository.findById(idTienda)
                .orElseThrow(() -> new ConfiguracionTiendaNoEncontradaException(
                        "La configuración visual de la tienda no ha sido inicializada. Debe ser creada por el proceso de registro de tienda."));

        return ConfiguracionTiendaMapper.toResponse(configuracion);
    }

    @Override
    @Transactional
    public ConfiguracionTiendaResponse actualizarConfiguracion(UUID idTienda, ActualizarConfiguracionRequest request) {
        TiendaInfo tienda = tiendaPort.obtenerTiendaPorId(idTienda)
                .orElseThrow(() -> new ConfiguracionTiendaNoEncontradaException(
                        "La tienda no existe o no se encuentra disponible"));

        tiendaStatePolicy.validarModificable(tienda.estado());

        ConfiguracionTienda configuracion = configuracionTiendaRepository.findById(idTienda)
                .orElseThrow(() -> new ConfiguracionTiendaNoEncontradaException(
                        "La configuración visual de la tienda no ha sido inicializada. Debe ser creada por el proceso de registro de tienda."));

        if (request.idPlantilla() != null) {
            Plantilla nuevaPlantilla = plantillaRepository.findById(request.idPlantilla())
                    .orElseThrow(() -> new PlantillaNoEncontradaException(
                            "No se encontró la plantilla con id: " + request.idPlantilla()));

            if (!Boolean.TRUE.equals(nuevaPlantilla.getActiva())) {
                throw new PlantillaInactivaException(
                        "No se puede seleccionar una plantilla inactiva (id: " + request.idPlantilla() + ")");
            }

            configuracion.setPlantilla(nuevaPlantilla);
        }

        if (request.tituloInicio() != null) {
            String titulo = request.tituloInicio().trim();
            if (titulo.isEmpty()) {
                throw new IllegalArgumentException("El título de inicio no puede ser solo espacios");
            }
            configuracion.setTituloInicio(titulo);
        }

        if (request.descripcionInicio() != null) {
            configuracion.setDescripcionInicio(request.descripcionInicio());
        }

        if (request.mostrarCarrusel() != null) {
            configuracion.setMostrarCarrusel(request.mostrarCarrusel());
        }

        configuracion.setFechaActualizacion(OffsetDateTime.now());

        ConfiguracionTienda guardada = configuracionTiendaRepository.saveAndFlush(configuracion);
        return ConfiguracionTiendaMapper.toResponse(guardada);
    }
}
