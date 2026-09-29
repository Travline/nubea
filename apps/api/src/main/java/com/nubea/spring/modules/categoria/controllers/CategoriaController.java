package com.nubea.spring.modules.categoria.controllers;

import com.nubea.spring.modules.categoria.dto.CategoriaRequest;
import com.nubea.spring.modules.categoria.dto.CategoriaResponse;
import com.nubea.spring.modules.categoria.entities.Categoria;
import com.nubea.spring.modules.categoria.services.CategoriaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarTodas() {
        return ResponseEntity.ok(categoriaService.listarTodas());
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscarPorId(
            @PathVariable Long id) {

        return categoriaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(
            @RequestBody CategoriaRequest request) {

        CategoriaResponse nuevaCategoria =
                categoriaService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(nuevaCategoria);
    }

    
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> actualizar(
            @PathVariable Long id,
            @RequestBody CategoriaRequest request) {

        try {
            CategoriaResponse categoriaActualizada =
                    categoriaService.actualizar(id, request);

            return ResponseEntity.ok(categoriaActualizada);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id) {

        try {
            categoriaService.eliminar(id);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}