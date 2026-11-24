package com.ecommerce.backend.catalogo.dto.response;

import java.util.List;

/**
 * Lista de requisitos de RN-M2-13 para publicar un producto, para mostrar el checklist sin intentar publicar.
 * Códigos: TIENDA_ACTIVA, CATEGORIA_ACTIVA, VERSION_ACTIVA, VERSION_CON_UNIDADES.
 */
public record RequisitosPublicacionResponse(boolean publicable, List<Requisito> requisitos) {
    public record Requisito(String codigo, boolean cumplido, String mensaje) {}
}
