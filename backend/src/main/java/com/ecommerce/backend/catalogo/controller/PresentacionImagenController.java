
package com.ecommerce.backend.catalogo.controller;

import com.ecommerce.backend.catalogo.dto.response.ImagenProductoResponse;
import com.ecommerce.backend.catalogo.dto.response.PresentacionAsociadaResponse;
import com.ecommerce.backend.catalogo.mapper.ImagenProductoMapper;
import com.ecommerce.backend.shared.storage.AlmacenamientoImagenes;
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

    private final AlmacenamientoImagenes almacenamiento;

    public PresentacionImagenController(
            PresentacionImagenService presentacionImagenService,
            AlmacenamientoImagenes almacenamiento) {
        this.presentacionImagenService = presentacionImagenService;
        this.almacenamiento = almacenamiento;
    }

    // Asociar una imagen a una presentación.
    @org.springframework.security.access.prepost.PreAuthorize("@tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeVer(#idTienda, authentication)")
    @GetMapping("/presentaciones/{idPresentacion}/imagenes")
    public ResponseEntity<List<ImagenProductoResponse>> listarImagenesPorPresentacion(
            @PathVariable UUID idTienda,
            @PathVariable UUID idPresentacion) {

        // Mismo contrato que GET /productos/{id}/imagenes: sin storageKey (se expone la URL pública).
        return ResponseEntity.ok(
                presentacionImagenService.listarImagenesPorPresentacion(idPresentacion, idTienda).stream()
                        .map(imagen -> ImagenProductoMapper.toResponse(imagen, almacenamiento.url(imagen.getStorageKey())))
                        .toList());
    }

    // Listar presentaciones asociadas a una imagen.
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeVer(#idTienda, authentication)")
    @GetMapping("/imagenes/{idImagen}/presentaciones")
    public ResponseEntity<List<PresentacionAsociadaResponse>> listarPresentacionesPorImagen(
            @PathVariable UUID idTienda,
            @PathVariable UUID idImagen) {

        // Sin idTienda ni firmaValores.
        return ResponseEntity.ok(
                presentacionImagenService.listarPresentacionesPorImagen(idImagen, idTienda).stream()
                        .map(p -> new PresentacionAsociadaResponse(p.getIdPresentacion(), p.getIdProducto(), p.getSku(),
                                p.getPrecioOverride(), p.getEstado()))
                        .toList());
    }

    // Desasociar una imagen de una presentación.
    @org.springframework.security.access.prepost.PreAuthorize("@tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
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
