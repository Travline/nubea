package com.ecommerce.backend.inventario.dto;

import jakarta.validation.constraints.NotNull;

/** Ajuste relativo del día a día: {@code +5} entran unidades, {@code -3} salen. */
public record AjusteInventarioRequest(@NotNull(message = "Indica cuántas unidades sumar o quitar") Integer cantidad) {}
