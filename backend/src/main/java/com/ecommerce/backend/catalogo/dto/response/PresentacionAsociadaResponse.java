package com.ecommerce.backend.catalogo.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Versión (presentación) asociada a una imagen. Solo datos para el panel del dueño:
 * no expone {@code idTienda} ni {@code firmaValores} (detalle interno de RN-M2-10).
 */
public record PresentacionAsociadaResponse(UUID idPresentacion, UUID idProducto, String sku,
                                           BigDecimal precioOverride, String estado) {}
