
package com.ecommerce.backend.catalogo.controller;

import com.ecommerce.backend.catalogo.dto.request.ColeccionRequest;
import com.ecommerce.backend.catalogo.dto.response.ColeccionResponse;
import com.ecommerce.backend.catalogo.service.ColeccionService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tiendas/{idTienda}/colecciones")
public class ColeccionController {

    private final ColeccionService coleccionService;

    public ColeccionController(ColeccionService coleccionService) {
        this.coleccionService = coleccionService;
    }

    // Crear una colección.
    @org.springframework.security.access.prepost.PreAuthorize("@tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
    @PostMapping
    public ResponseEntity<ColeccionResponse> crearColeccion(
            @PathVariable UUID idTienda,
            @Valid @RequestBody ColeccionRequest request) {

        ColeccionResponse response =
                coleccionService.crearColeccion(idTienda, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Listar todas las colecciones.
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeVer(#idTienda, authentication)")
    @GetMapping
    public ResponseEntity<List<ColeccionResponse>> listarColecciones(
            @PathVariable UUID idTienda) {

        return ResponseEntity.ok(
                coleccionService.listarColecciones(idTienda));
    }

    // Listar únicamente las colecciones activas.
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeVer(#idTienda, authentication)")
    @GetMapping("/activas")
    public ResponseEntity<List<ColeccionResponse>> listarColeccionesActivas(
            @PathVariable UUID idTienda) {

        return ResponseEntity.ok(
                coleccionService.listarColeccionesActivas(idTienda));
    }

    // Obtener una colección por su ID.
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeVer(#idTienda, authentication)")
    @GetMapping("/{idColeccion}")
    public ResponseEntity<ColeccionResponse> obtenerColeccionPorId(
            @PathVariable UUID idTienda,
            @PathVariable UUID idColeccion) {

        return ResponseEntity.ok(
                coleccionService.obtenerColeccionPorId(
                        idColeccion, idTienda));
    }

    // Actualizar una colección.
    @org.springframework.security.access.prepost.PreAuthorize("@tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
    @PutMapping("/{idColeccion}")
    public ResponseEntity<ColeccionResponse> actualizarColeccion(
            @PathVariable UUID idTienda,
            @PathVariable UUID idColeccion,
            @Valid @RequestBody ColeccionRequest request) {

        return ResponseEntity.ok(
                coleccionService.actualizarColeccion(
                        idColeccion, idTienda, request));
    }

    // Cambiar el estado de una colección.
    @org.springframework.security.access.prepost.PreAuthorize("@tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
    @PatchMapping("/{idColeccion}/estado")
    public ResponseEntity<ColeccionResponse> cambiarEstadoColeccion(
            @PathVariable UUID idTienda,
            @PathVariable UUID idColeccion,
            @RequestParam Boolean activa) {

        return ResponseEntity.ok(
                coleccionService.cambiarEstadoColeccion(
                        idColeccion, idTienda, activa));
    }
}
