
package com.ecommerce.backend.catalogo.controller;

import com.ecommerce.backend.catalogo.model.ImagenProducto;
import com.ecommerce.backend.catalogo.model.PresentacionProducto;
import com.ecommerce.backend.catalogo.service.PresentacionImagenService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tiendas/{idTienda}")
public class PresentacionImagenController {

    private final PresentacionImagenService presentacionImagenService;

    public PresentacionImagenController(
            PresentacionImagenService presentacionImagenService) {
        this.presentacionImagenService = presentacionImagenService;
    }

    // Asociar una imagen a una presentación.
    @PostMapping("/presentaciones/{idPresentacion}/imagenes/{idImagen}")
    public ResponseEntity<Void> asociarImagen(
            @PathVariable UUID idTienda,
            @PathVariable UUID idPresentacion,
            @PathVariable UUID idImagen) {

        presentacionImagenService.asociarImagen(
                idPresentacion, idImagen, idTienda);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Listar imágenes asociadas a una presentación.
    @GetMapping("/presentaciones/{idPresentacion}/imagenes")
    public ResponseEntity<List<ImagenProducto>> listarImagenesPorPresentacion(
            @PathVariable UUID idTienda,
            @PathVariable UUID idPresentacion) {

        return ResponseEntity.ok(
                presentacionImagenService.listarImagenesPorPresentacion(
                        idPresentacion, idTienda));
    }

    // Listar presentaciones asociadas a una imagen.
    @GetMapping("/imagenes/{idImagen}/presentaciones")
    public ResponseEntity<List<PresentacionProducto>> listarPresentacionesPorImagen(
            @PathVariable UUID idTienda,
            @PathVariable UUID idImagen) {

        return ResponseEntity.ok(
                presentacionImagenService.listarPresentacionesPorImagen(
                        idImagen, idTienda));
    }

    // Desasociar una imagen de una presentación.
    @DeleteMapping("/presentaciones/{idPresentacion}/imagenes/{idImagen}")
    public ResponseEntity<Void> desasociarImagen(
            @PathVariable UUID idTienda,
            @PathVariable UUID idPresentacion,
            @PathVariable UUID idImagen) {

        presentacionImagenService.desasociarImagen(
                idPresentacion, idImagen, idTienda);

        return ResponseEntity.noContent().build();
    }
}
