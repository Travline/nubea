package com.ecommerce.backend.tienda.controller;

import com.ecommerce.backend.tienda.dto.CrearTiendaRequest;
import com.ecommerce.backend.tienda.dto.TiendaResponse;
import com.ecommerce.backend.tienda.service.TiendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.UUID;

@RestController 
@RequestMapping ("/api/tiendas")
@RequiredArgsConstructor
public class TiendaController {
  private final TiendaService tiendaService;    

   @PostMapping
    public ResponseEntity<TiendaResponse> crear(@Valid @RequestBody CrearTiendaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tiendaService.crearTienda(req));
    }

    // Nos ayuda a ver una tienda por su URL
    @GetMapping("/{slug}")
    public ResponseEntity<TiendaResponse> obtener(@PathVariable String slug) {
        return ResponseEntity.ok(tiendaService.obtenerPorSlug(slug));
    }

    // sirve para mostrar mis tiendas por usuario
    @GetMapping("/propietario/{idUsuario}")
    public ResponseEntity<List<TiendaResponse>> listarPorPropietario(@PathVariable UUID idUsuario) {
        return ResponseEntity.ok(tiendaService.listarPorPropietario(idUsuario));
    }
    
    


    
}
