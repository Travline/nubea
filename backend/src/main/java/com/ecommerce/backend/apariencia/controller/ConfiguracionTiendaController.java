package com.ecommerce.backend.apariencia.controller;

import com.ecommerce.backend.apariencia.dto.ActualizarConfiguracionRequest;
import com.ecommerce.backend.apariencia.dto.ConfiguracionTiendaResponse;
import com.ecommerce.backend.apariencia.service.ConfiguracionTiendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/tiendas/{idTienda}/configuracion")
@RequiredArgsConstructor
public class ConfiguracionTiendaController {

    private final ConfiguracionTiendaService configuracionTiendaService;

    @GetMapping
    @PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")
    public ResponseEntity<ConfiguracionTiendaResponse> obtenerConfiguracion(@PathVariable UUID idTienda) {
        return ResponseEntity.ok(configuracionTiendaService.obtenerConfiguracion(idTienda));
    }

    @PatchMapping
    @PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")
    public ResponseEntity<ConfiguracionTiendaResponse> actualizarConfiguracion(
            @PathVariable UUID idTienda,
            @RequestBody @Valid ActualizarConfiguracionRequest request) {
        return ResponseEntity.ok(configuracionTiendaService.actualizarConfiguracion(idTienda, request));
    }
}
