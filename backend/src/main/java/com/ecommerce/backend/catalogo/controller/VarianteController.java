
package com.ecommerce.backend.catalogo.controller;

import com.ecommerce.backend.catalogo.dto.request.VarianteRequest;
import com.ecommerce.backend.catalogo.dto.response.VarianteResponse;
import com.ecommerce.backend.catalogo.service.VarianteService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tiendas/{idTienda}/variantes")
public class VarianteController {

    private final VarianteService varianteService;

    public VarianteController(VarianteService varianteService) {
        this.varianteService = varianteService;
    }

    // Crear variante.
    @PostMapping
    public ResponseEntity<VarianteResponse> crearVariante(
            @PathVariable UUID idTienda,
            @Valid @RequestBody VarianteRequest request) {

        VarianteResponse response =
                varianteService.crearVariante(idTienda, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Listar variantes de una tienda.
    @GetMapping
    public ResponseEntity<List<VarianteResponse>> listarVariantes(
            @PathVariable UUID idTienda) {

        List<VarianteResponse> response =
                varianteService.listarVariantes(idTienda);

        return ResponseEntity.ok(response);
    }

    // Obtener variante por ID.
    @GetMapping("/{idVariante}")
    public ResponseEntity<VarianteResponse> obtenerVariantePorId(
            @PathVariable UUID idTienda,
            @PathVariable UUID idVariante) {

        VarianteResponse response =
                varianteService.obtenerVariantePorId(
                        idVariante, idTienda);

        return ResponseEntity.ok(response);
    }

    // Actualizar variante.
    @PutMapping("/{idVariante}")
    public ResponseEntity<VarianteResponse> actualizarVariante(
            @PathVariable UUID idTienda,
            @PathVariable UUID idVariante,
            @Valid @RequestBody VarianteRequest request) {

        VarianteResponse response =
                varianteService.actualizarVariante(
                        idVariante, idTienda, request);

        return ResponseEntity.ok(response);
    }
}
