package com.ecommerce.backend.tienda.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearTiendaRequest(
    //TEMPORAL PARA PROBAR JSON el id sale del Token
    @NotNull(message = "El id del Propietario es obligatorio")
    String idPropietario,

    @NotBlank(message = "El nombre de la tienda es obligatorio")
    @Size(max = 150)
    String nombreß
) {
    
    
}
