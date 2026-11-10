
package com.ecommerce.backend.catalogo.controller;

import com.ecommerce.backend.catalogo.dto.request.CategoriaRequest;
import com.ecommerce.backend.catalogo.dto.response.CategoriaResponse;
import com.ecommerce.backend.catalogo.service.CategoriaService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tiendas/{idTienda}/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // Crear categoría.
    @PostMapping
    public ResponseEntity<CategoriaResponse> crearCategoria(
            @PathVariable UUID idTienda,
            @Valid @RequestBody CategoriaRequest request) {

        CategoriaResponse response =
                categoriaService.crearCategoria(idTienda, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Listar categorías de una tienda.
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarCategorias(
            @PathVariable UUID idTienda) {

        List<CategoriaResponse> response =
                categoriaService.listarCategorias(idTienda);

        return ResponseEntity.ok(response);
    }

    // Obtener categoría por ID.
    @GetMapping("/{idCategoria}")
    public ResponseEntity<CategoriaResponse> obtenerCategoriaPorId(
            @PathVariable UUID idTienda,
            @PathVariable UUID idCategoria) {

        CategoriaResponse response =
                categoriaService.obtenerCategoriaPorId(
                        idCategoria, idTienda);

        return ResponseEntity.ok(response);
    }

    // Actualizar categoría.
    @PutMapping("/{idCategoria}")
    public ResponseEntity<CategoriaResponse> actualizarCategoria(
            @PathVariable UUID idTienda,
            @PathVariable UUID idCategoria,
            @Valid @RequestBody CategoriaRequest request) {

        CategoriaResponse response =
                categoriaService.actualizarCategoria(
                        idCategoria, idTienda, request);

        return ResponseEntity.ok(response);
    }

    // Cambiar estado de la categoría.
    @PatchMapping("/{idCategoria}/estado")
    public ResponseEntity<CategoriaResponse> cambiarEstadoCategoria(
            @PathVariable UUID idTienda,
            @PathVariable UUID idCategoria,
            @RequestParam Boolean activa) {

        CategoriaResponse response =
                categoriaService.cambiarEstadoCategoria(
                        idCategoria, idTienda, activa);

        return ResponseEntity.ok(response);
    }
}
