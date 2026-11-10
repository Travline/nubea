package com.ecommerce.backend.identidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroUsuarioRequest(
    @NotBlank(message = "Los nombres son obligatorios")
    @Size (max = 100)
    String nombres,

    @Size(max = 80) String apellidoPaterno,
    @Size(max = 80) String apellidoMaterno,

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Size(max = 100)
    String correoElectronico,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    String password,

    @Size(max = 20) 
    String telefono,
    
    String tipoDocumento,
    @Size(max = 30) String numeroDocumento,
    //Despues creamos nuestra validacion personalizada para el tipo de documento y el numero de documento
    @Pattern(regexp = "^[A-Z]{2}$", message = "El tipo de documento debe ser un codigo de dos Letras (por ejemplo, 'DNI', 'PAS', 'RUC')")
    String paisEmisorDocumento 
) {}
