package com.ecommerce.backend.inventario.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * {@code stockActual}: unidades disponibles para vender. {@code stockReservado} (nuevo, opcional para el cliente):
 * unidades apartadas en pedidos pendientes de pago.
 */
public record InventarioResponse(UUID idProducto, String nombreProducto, String estadoProducto,
        UUID idPresentacion, String sku, String estadoPresentacion, Integer stockActual,
        Integer stockMinimo, OffsetDateTime fechaActualizacion, boolean sinRegistroInventario,
        Integer stockReservado) {

    /** Firma anterior, sin {@code stockReservado}. */
    public InventarioResponse(UUID idProducto, String nombreProducto, String estadoProducto,
            UUID idPresentacion, String sku, String estadoPresentacion, Integer stockActual,
            Integer stockMinimo, OffsetDateTime fechaActualizacion, boolean sinRegistroInventario) {
        this(idProducto, nombreProducto, estadoProducto, idPresentacion, sku, estadoPresentacion, stockActual,
                stockMinimo, fechaActualizacion, sinRegistroInventario, 0);
    }
}
