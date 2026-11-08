package com.ecommerce.backend.inventario.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InventarioResponse(UUID idProducto, String nombreProducto, String estadoProducto,
        UUID idPresentacion, String sku, String estadoPresentacion, Integer stockActual,
        Integer stockMinimo, OffsetDateTime fechaActualizacion, boolean sinRegistroInventario) {}
