package com.ecommerce.backend.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarInventarioRequest(@NotNull @Min(0) Integer stockActual,
                                           @NotNull @Min(0) Integer stockMinimo) {}
