package com.ecommerce.backend.shared.tienda;

import java.util.UUID;

public record TiendaInfo(
    UUID idTienda,
    UUID idPropietario,
    String estado,
    String nombre
) {}
