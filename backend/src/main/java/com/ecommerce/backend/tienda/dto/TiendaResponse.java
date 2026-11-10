package com.ecommerce.backend.tienda.dto;
import java.time.OffsetDateTime;
import java.util.UUID;  

public record TiendaResponse(
    UUID idTienda,
    UUID idPropietario,
    String nombre,
    String slug,
    String estado,
    String plantilla,
    OffsetDateTime fechaCreacion
) {
    
}
