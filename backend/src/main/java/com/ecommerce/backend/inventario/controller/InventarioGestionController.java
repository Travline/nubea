package com.ecommerce.backend.inventario.controller;

import com.ecommerce.backend.inventario.dto.ActualizarInventarioRequest;
import com.ecommerce.backend.inventario.dto.AjusteInventarioRequest;
import com.ecommerce.backend.inventario.dto.InventarioActualResponse;
import com.ecommerce.backend.inventario.service.AjusteInventarioService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Modificación manual del inventario. Solo el dueño, con la tienda editable (el admin no edita contenido).
 * {@code stockActual} son unidades disponibles; las apartadas son solo las de pedidos pendientes de pago
 * (mientras no exista el módulo de envíos, un pedido PAGADO se considera entregado).
 */
@RestController
@RequestMapping("/api/tiendas/{idTienda}/inventario")
@RequiredArgsConstructor
public class InventarioGestionController {
    private final AjusteInventarioService inventario;

    /** "Fijar stock" tras un conteo: unidades en mano − apartadas. Responde 204 como antes. */
    @PutMapping("/{idPresentacion}")
    @PreAuthorize("@tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
    public ResponseEntity<Void> actualizar(@PathVariable UUID idTienda, @PathVariable UUID idPresentacion,
                                           @Valid @RequestBody ActualizarInventarioRequest request) {
        inventario.fijar(idTienda, idPresentacion, request);
        return ResponseEntity.noContent().build();
    }

    /** Ajuste relativo del día a día: {@code {"cantidad": 5}} o {@code {"cantidad": -3}}. */
    @PostMapping("/{idPresentacion}/ajustes")
    @PreAuthorize("@tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
    public InventarioActualResponse ajustar(@PathVariable UUID idTienda, @PathVariable UUID idPresentacion,
                                            @Valid @RequestBody AjusteInventarioRequest request) {
        return inventario.ajustar(idTienda, idPresentacion, request.cantidad());
    }
}
