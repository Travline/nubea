package com.ecommerce.backend.banner.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReordenarBannersRequest(
    @NotEmpty(message = "La lista de banners a reordenar no puede estar vacía")
    @Valid
    List<ItemOrden> banners
) {
    public record ItemOrden(
        @NotNull(message = "El idBanner es obligatorio")
        UUID idBanner,

        @NotNull(message = "El orden es obligatorio")
        @Min(value = 1, message = "El orden debe ser un número entero positivo mayor a cero")
        Short orden
    ) {}
}
