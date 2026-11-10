package com.ecommerce.backend.identidad.dto;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record UsuarioResponse(
    UUID idUsuario,
    String nombre,
    String apellidoPaterno,
    String apellidoMaterno,
    String correo,
    String estado,
    Set<String> roles,
    OffsetDateTime fechaCreacion
) {
    
}
