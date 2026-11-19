package com.ecommerce.backend.identidad.dto;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record UsuarioResponse(
    UUID idUsuario,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    String correoElectronico,
    String estado,
    Set<String> roles,
    OffsetDateTime fechaCreacion
) {
    
}
