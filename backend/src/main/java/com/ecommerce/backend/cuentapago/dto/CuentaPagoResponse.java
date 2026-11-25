package com.ecommerce.backend.cuentapago.dto;

import java.time.OffsetDateTime;

/**
 * {@code ambientePago} indica PRUEBA o PRODUCCION según la configuración del checkout, en modo
 * global o por tienda. Con el checkout deshabilitado para esa tienda, indica el último ambiente
 * en que cobró o {@code null} si aún no tiene cobros.
 */
public record CuentaPagoResponse(String proveedor, String estado, String idCuentaExterna,
                                OffsetDateTime fechaVinculacion, OffsetDateTime fechaActualizacion, String ambientePago) {
    public CuentaPagoResponse(String proveedor, String estado, String idCuentaExterna,
                              OffsetDateTime fechaVinculacion, OffsetDateTime fechaActualizacion) {
        this(proveedor, estado, idCuentaExterna, fechaVinculacion, fechaActualizacion, null);
    }
}
