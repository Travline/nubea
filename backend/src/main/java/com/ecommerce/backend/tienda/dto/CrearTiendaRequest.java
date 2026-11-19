package com.ecommerce.backend.tienda.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearTiendaRequest(
    //TEMPORAL PARA PROBAR JSON el id sale del Token
    UUID idPropietario,

    @NotBlank(message = "El nombre de la tienda es obligatorio")
    @Size(max = 150)
    String nombre
) {
    
    
}
