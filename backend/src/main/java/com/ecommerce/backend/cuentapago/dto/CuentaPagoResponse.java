package com.ecommerce.backend.cuentapago.dto;

import java.time.OffsetDateTime;

/**
 * {@code ambientePago}: PRUEBA | PRODUCCION. Se toma de la configuración de cobros de la tienda
 * (MERCADOPAGO_PAGOS_TIENDAS), que es la que usa el checkout; si la tienda no está habilitada, el último
 * ambiente con el que cobró; si no hay ninguno, null. Sin datos sensibles.
 */
public record CuentaPagoResponse(String proveedor, String estado, String idCuentaExterna,
                                OffsetDateTime fechaVinculacion, OffsetDateTime fechaActualizacion, String ambientePago) {
    public CuentaPagoResponse(String proveedor, String estado, String idCuentaExterna,
                              OffsetDateTime fechaVinculacion, OffsetDateTime fechaActualizacion) {
        this(proveedor, estado, idCuentaExterna, fechaVinculacion, fechaActualizacion, null);
    }
}
