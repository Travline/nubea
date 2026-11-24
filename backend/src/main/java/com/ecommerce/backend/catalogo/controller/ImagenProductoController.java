
package com.ecommerce.backend.catalogo.controller;

import com.ecommerce.backend.catalogo.dto.request.ImagenProductoRequest;
import com.ecommerce.backend.catalogo.dto.response.ImagenProductoResponse;
import com.ecommerce.backend.catalogo.service.ImagenProductoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagenProductoResponse> crearImagen(
            @PathVariable UUID idTienda,
            @PathVariable UUID idProducto,
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam Integer orden,
            @RequestParam(defaultValue = "false") Boolean esPrincipal) {

        ImagenProductoResponse response =
                imagenProductoService.crearImagen(
                        idProducto, idTienda, archivo, orden, esPrincipal);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
    @PutMapping(value = "/{idImagen}/archivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagenProductoResponse> sustituirImagen(@PathVariable UUID idTienda,
            @PathVariable UUID idProducto, @PathVariable UUID idImagen, @RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(imagenProductoService.sustituirImagen(idImagen, idProducto, idTienda, archivo));
    }

    // Listar imágenes del producto.
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeVer(#idTienda, authentication)")
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeVer(#idTienda, authentication)")
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
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
