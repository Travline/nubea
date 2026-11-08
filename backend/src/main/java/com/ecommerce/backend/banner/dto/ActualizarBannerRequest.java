package com.ecommerce.backend.banner.dto;

public record ActualizarBannerRequest(
    String enlaceDestino,
    Boolean activo
) {}
