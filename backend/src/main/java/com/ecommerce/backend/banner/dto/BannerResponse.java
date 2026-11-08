package com.ecommerce.backend.banner.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BannerResponse(
    UUID idBanner,
    UUID idTienda,
    String enlaceDestino,
    Short orden,
    Boolean activo,
    OffsetDateTime fechaCreacion
) {}
