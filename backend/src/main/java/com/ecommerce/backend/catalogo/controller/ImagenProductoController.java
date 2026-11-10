
package com.ecommerce.backend.catalogo.controller;

import com.ecommerce.backend.catalogo.dto.request.ImagenProductoRequest;
import com.ecommerce.backend.catalogo.dto.response.ImagenProductoResponse;
import com.ecommerce.backend.catalogo.service.ImagenProductoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
    "/api/tiendas/{idTienda}/productos/{idProducto}/imagenes"
)
public class ImagenProductoController {

    private final ImagenProductoService imagenProductoService;

    public ImagenProductoController(
            ImagenProductoService imagenProductoService) {
        this.imagenProductoService = imagenProductoService;
    }

    // Crear imagen.
    @PostMapping
    public ResponseEntity<ImagenProductoResponse> crearImagen(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @Valid @RequestBody ImagenProductoRequest request) {

        ImagenProductoResponse response =
                imagenProductoService.crearImagen(
                        idProducto, idTienda, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Listar imágenes del producto.
    @GetMapping
    public ResponseEntity<List<ImagenProductoResponse>> listarImagenes(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto) {

        List<ImagenProductoResponse> response =
                imagenProductoService.listarImagenes(
                        idProducto, idTienda);

        return ResponseEntity.ok(response);
    }

    // Obtener imagen por ID.
    @GetMapping("/{idImagen}")
    public ResponseEntity<ImagenProductoResponse> obtenerImagenPorId(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @PathVariable UUID idImagen) {

        ImagenProductoResponse response =
                imagenProductoService.obtenerImagenPorId(
                        idImagen, idProducto, idTienda);

        return ResponseEntity.ok(response);
    }

    // Actualizar información de una imagen.
    @PutMapping("/{idImagen}")
    public ResponseEntity<ImagenProductoResponse> actualizarImagen(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @PathVariable UUID idImagen,
            @Valid @RequestBody ImagenProductoRequest request) {

        ImagenProductoResponse response =
                imagenProductoService.actualizarImagen(
                        idImagen, idProducto, idTienda, request);

        return ResponseEntity.ok(response);
    }

    // Establecer imagen principal.
    @PatchMapping("/{idImagen}/principal")
    public ResponseEntity<ImagenProductoResponse> establecerImagenPrincipal(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @PathVariable UUID idImagen) {

        ImagenProductoResponse response =
                imagenProductoService.establecerImagenPrincipal(
                        idImagen, idProducto, idTienda);

        return ResponseEntity.ok(response);
    }

    // Eliminar imagen.
    @DeleteMapping("/{idImagen}")
    public ResponseEntity<Void> eliminarImagen(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @PathVariable UUID idImagen) {

        imagenProductoService.eliminarImagen(
                idImagen, idProducto, idTienda);

        return ResponseEntity.noContent().build();
    }
}
