package com.ecommerce.backend.plantilla.controller;

import com.ecommerce.backend.plantilla.dto.CrearPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.PlantillaResponse;
import com.ecommerce.backend.plantilla.service.PlantillaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plantillas")
@RequiredArgsConstructor
public class PlantillaController {

    private final PlantillaService plantillaService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_GENERAL')")
    public ResponseEntity<PlantillaResponse> crearPlantilla(@RequestBody @Valid CrearPlantillaRequest request) {
        PlantillaResponse response = plantillaService.crearPlantilla(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
