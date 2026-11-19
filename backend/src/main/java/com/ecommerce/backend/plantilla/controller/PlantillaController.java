package com.ecommerce.backend.plantilla.controller;

import com.ecommerce.backend.plantilla.dto.ActualizarEstadoPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.ActualizarPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.CrearPlantillaRequest;
import com.ecommerce.backend.plantilla.dto.PlantillaResponse;
import com.ecommerce.backend.plantilla.service.PlantillaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PatchMapping("/{idPlantilla}")
    @PreAuthorize("hasRole('ADMIN_GENERAL')")
    public ResponseEntity<PlantillaResponse> actualizarPlantilla(
            @PathVariable Short idPlantilla,
            @RequestBody @Valid ActualizarPlantillaRequest request) {
        PlantillaResponse response = plantillaService.actualizarPlantilla(idPlantilla, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{idPlantilla}/estado")
    @PreAuthorize("hasRole('ADMIN_GENERAL')")
    public ResponseEntity<PlantillaResponse> actualizarEstadoPlantilla(
            @PathVariable Short idPlantilla,
            @RequestBody @Valid ActualizarEstadoPlantillaRequest request) {
        PlantillaResponse response = plantillaService.actualizarEstadoPlantilla(idPlantilla, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL', 'DUENO_TIENDA')")
    public ResponseEntity<List<PlantillaResponse>> listarPlantillas(
            @RequestParam(name = "incluirInactivas", required = false, defaultValue = "false") Boolean incluirInactivas) {
        return ResponseEntity.ok(plantillaService.listarPlantillas(incluirInactivas));
    }

    @GetMapping("/{idPlantilla}")
    @PreAuthorize("hasAnyRole('ADMIN_GENERAL', 'DUENO_TIENDA')")
    public ResponseEntity<PlantillaResponse> obtenerPorId(@PathVariable Short idPlantilla) {
        return ResponseEntity.ok(plantillaService.obtenerPorId(idPlantilla));
    }
}
