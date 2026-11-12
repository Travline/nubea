
package com.ecommerce.backend.catalogo.controller;

import com.ecommerce.backend.catalogo.dto.request.ValorVarianteRequest;
import com.ecommerce.backend.catalogo.dto.response.ValorVarianteResponse;
import com.ecommerce.backend.catalogo.service.ValorVarianteService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tiendas/{idTienda}/variantes/{idVariante}/valores")
public class ValorVarianteController {

    private final ValorVarianteService valorVarianteService;

    public ValorVarianteController(
            ValorVarianteService valorVarianteService) {
        this.valorVarianteService = valorVarianteService;
    }

    // Crear valor de variante.
    @PostMapping
    public ResponseEntity<ValorVarianteResponse> crearValorVariante(
            @PathVariable UUID idTienda,
            @PathVariable UUID idVariante,
            @Valid @RequestBody ValorVarianteRequest request) {

        ValorVarianteResponse response =
                valorVarianteService.crearValorVariante(
                        idVariante, idTienda, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Listar valores de una variante.
    @GetMapping
    public ResponseEntity<List<ValorVarianteResponse>> listarValores(
            @PathVariable UUID idTienda,
            @PathVariable UUID idVariante) {

        List<ValorVarianteResponse> response =
                valorVarianteService.listarValores(
                        idVariante, idTienda);

        return ResponseEntity.ok(response);
    }

    // Obtener un valor por ID.
    @GetMapping("/{idValor}")
    public ResponseEntity<ValorVarianteResponse> obtenerValorPorId(
            @PathVariable UUID idTienda,
            @PathVariable UUID idVariante,
            @PathVariable UUID idValor) {

        ValorVarianteResponse response =
                valorVarianteService.obtenerValorPorId(
                        idValor, idVariante, idTienda);

        return ResponseEntity.ok(response);
    }

    // Actualizar un valor.
    @PutMapping("/{idValor}")
    public ResponseEntity<ValorVarianteResponse> actualizarValor(
            @PathVariable UUID idTienda,
            @PathVariable UUID idVariante,
            @PathVariable UUID idValor,
            @Valid @RequestBody ValorVarianteRequest request) {

        ValorVarianteResponse response =
                valorVarianteService.actualizarValor(
                        idValor, idVariante, idTienda, request);

        return ResponseEntity.ok(response);
    }
}
