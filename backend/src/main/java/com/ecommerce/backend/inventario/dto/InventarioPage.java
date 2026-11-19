package com.ecommerce.backend.inventario.dto;

import java.util.List;

public record InventarioPage(List<InventarioResponse> contenido, int page, int size,
                             long totalElementos, int totalPaginas) {}
