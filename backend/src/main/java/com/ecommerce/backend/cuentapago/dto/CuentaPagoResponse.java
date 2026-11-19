package com.ecommerce.backend.cuentapago.dto;

import java.time.OffsetDateTime;

public record CuentaPagoResponse(String proveedor, String estado, String idCuentaExterna,
                                OffsetDateTime fechaVinculacion, OffsetDateTime fechaActualizacion) {}
