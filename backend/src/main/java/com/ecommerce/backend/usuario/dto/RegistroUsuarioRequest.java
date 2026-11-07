package com.ecommerce.backend.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroUsuarioRequest (
    
    @NotBlank (message = "Los nombres son obligatorios")
    String nombres,

    @NotBlank (message = "Los apellidos son obligatorios")
    String apellidos,

    @NotBlank (message = "El correo es obligatorio")
    @Email (message = "El correo no tiene un formato Valido")
    String correo,

    @NotBlank (message = "La contraseña es obligatoria")
    @Size (min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    String contrasena


){}

    
    

