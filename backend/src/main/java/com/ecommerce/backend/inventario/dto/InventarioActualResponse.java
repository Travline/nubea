package com.ecommerce.backend.inventario.dto;

import java.util.UUID;

/**
 * Stock de una versión tras un ajuste.
 * {@code stockActual}: unidades disponibles para vender. {@code stockReservado}: apartadas en pedidos
 * pendientes de pago. Mientras no exista el módulo de envíos, un pedido PAGADO se considera entregado.
 */
public record InventarioActualResponse(UUID idPresentacion, int stockActual, int stockMinimo, int stockReservado) {}
