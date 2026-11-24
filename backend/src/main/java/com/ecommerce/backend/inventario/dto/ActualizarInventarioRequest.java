package com.ecommerce.backend.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * "Fijar stock" tras un conteo.
 * {@code stockActual}: unidades en mano contadas por el vendedor. El backend guarda como disponibles
 * {@code stockActual − apartadas}, donde las apartadas son solo las de pedidos pendientes de pago
 * (mientras no exista el módulo de envíos, un pedido PAGADO se considera entregado).
 * {@code stockEsperado} (opcional): las unidades disponibles que el vendedor veía al editar; si ya no coinciden,
 * se responde 409 para no pisar una venta o un ajuste simultáneo.
 */
public record ActualizarInventarioRequest(@NotNull(message = "Indica las unidades contadas")
                                          @Min(value = 0, message = "Las unidades no pueden ser negativas") Integer stockActual,
                                          @NotNull(message = "Indica el mínimo de unidades")
                                          @Min(value = 0, message = "Las unidades no pueden ser negativas") Integer stockMinimo,
                                          @Min(value = 0, message = "Las unidades no pueden ser negativas") Integer stockEsperado) {}
