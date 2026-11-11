package com.ecommerce.backend.tienda.controller;

import com.ecommerce.backend.tienda.dto.CrearTiendaRequest;
import com.ecommerce.backend.tienda.dto.TiendaResponse;
import com.ecommerce.backend.tienda.service.TiendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping ("/api/tiendas")
@RequiredArgsConstructor
public class TiendaController {
  private final TiendaService tiendaService;    

   @PostMapping
    public ResponseEntity<TiendaResponse> crear(@Valid @RequestBody CrearTiendaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tiendaService.crearTienda(req));
    }
}
