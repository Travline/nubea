package com.ecommerce.backend.apariencia.service;

import com.ecommerce.backend.apariencia.dto.ActualizarConfiguracionRequest;
import com.ecommerce.backend.apariencia.dto.ConfiguracionTiendaResponse;

import java.util.UUID;

public interface ConfiguracionTiendaService {
    ConfiguracionTiendaResponse obtenerConfiguracion(UUID idTienda);
    ConfiguracionTiendaResponse actualizarConfiguracion(UUID idTienda, ActualizarConfiguracionRequest request);
}
