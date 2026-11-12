
package com.ecommerce.backend.catalogo.controller;

import com.ecommerce.backend.catalogo.dto.request.PresentacionProductoRequest;
import com.ecommerce.backend.catalogo.dto.response.PresentacionProductoResponse;
import com.ecommerce.backend.catalogo.service.PresentacionProductoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
    "/api/tiendas/{idTienda}/productos/{idProducto}/presentaciones"
)
public class PresentacionProductoController {

    private final PresentacionProductoService presentacionProductoService;

    public PresentacionProductoController(
            PresentacionProductoService presentacionProductoService) {
        this.presentacionProductoService = presentacionProductoService;
    }

    // Crear presentación.
    @PostMapping
    public ResponseEntity<PresentacionProductoResponse> crearPresentacion(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @Valid @RequestBody PresentacionProductoRequest request) {

        PresentacionProductoResponse response =
                presentacionProductoService.crearPresentacion(
                        idProducto, idTienda, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Listar presentaciones de un producto.
    @GetMapping
    public ResponseEntity<List<PresentacionProductoResponse>> listarPresentaciones(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto) {

        List<PresentacionProductoResponse> response =
                presentacionProductoService.listarPresentaciones(
                        idProducto, idTienda);

        return ResponseEntity.ok(response);
    }

    // Obtener presentación por ID.
    @GetMapping("/{idPresentacion}")
    public ResponseEntity<PresentacionProductoResponse> obtenerPresentacionPorId(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @PathVariable UUID idPresentacion) {

        PresentacionProductoResponse response =
                presentacionProductoService.obtenerPresentacionPorId(
                        idPresentacion, idProducto, idTienda);

        return ResponseEntity.ok(response);
    }

    // Actualizar presentación.
    @PutMapping("/{idPresentacion}")
    public ResponseEntity<PresentacionProductoResponse> actualizarPresentacion(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @PathVariable UUID idPresentacion,
            @Valid @RequestBody PresentacionProductoRequest request) {

        PresentacionProductoResponse response =
                presentacionProductoService.actualizarPresentacion(
                        idPresentacion, idProducto, idTienda, request);

        return ResponseEntity.ok(response);
    }

    // Cambiar estado de la presentación.
    @PatchMapping("/{idPresentacion}/estado")
    public ResponseEntity<PresentacionProductoResponse> cambiarEstadoPresentacion(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @PathVariable UUID idPresentacion,
            @RequestParam String estado) {

        PresentacionProductoResponse response =
                presentacionProductoService.cambiarEstadoPresentacion(
                        idPresentacion, idProducto, idTienda, estado);

        return ResponseEntity.ok(response);
    }

    // Actualizar valores de variantes de una presentación.
    @PutMapping("/{idPresentacion}/valores")
    public ResponseEntity<PresentacionProductoResponse> actualizarValoresPresentacion(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @PathVariable UUID idPresentacion,
            @RequestBody List<UUID> idsValores) {

        PresentacionProductoResponse response =
                presentacionProductoService.actualizarValoresPresentacion(
                        idPresentacion, idProducto, idTienda, idsValores);

        return ResponseEntity.ok(response);
    }
}
