package com.ecommerce.backend.tienda.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tienda vista por el ADMIN_GENERAL: como {@link TiendaResponse}, más el dueño legible (nombre y correo) para que el
 * panel de administración no muestre identificadores. Solo se usa en /api/admin/tiendas.
 */
public record TiendaAdministracionResponse(
        UUID idTienda,
        String nombre,
        String slug,
        String estado,
        String plantilla,
        OffsetDateTime fechaCreacion,
        UUID idPropietario,
        String nombrePropietario,
        String correoPropietario) {
}
