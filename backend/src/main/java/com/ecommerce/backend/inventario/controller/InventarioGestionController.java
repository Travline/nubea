package com.ecommerce.backend.inventario.controller;

import com.ecommerce.backend.inventario.dto.ActualizarInventarioRequest;
import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tiendas/{idTienda}/inventario")
@RequiredArgsConstructor
public class InventarioGestionController {
    private final JdbcTemplate jdbc;

    @PutMapping("/{idPresentacion}")
    @PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.puedeEditar(#idTienda, authentication)")
    @Transactional
    public ResponseEntity<Void> actualizar(@PathVariable UUID idTienda, @PathVariable UUID idPresentacion,
                                           @Valid @RequestBody ActualizarInventarioRequest request) {
        int count = jdbc.update("""
                UPDATE inventario i SET stock_actual = ?, stock_minimo = ?, fecha_actualizacion = now()
                FROM presentacion_producto p WHERE p.id_presentacion = i.id_presentacion
                AND p.id_tienda = ? AND i.id_presentacion = ?
                """, request.stockActual(), request.stockMinimo(), idTienda, idPresentacion);
        if (count == 0) throw new RecursoNoEncontradoException("Inventario no encontrado en esta tienda");
        return ResponseEntity.noContent().build();
    }
}
