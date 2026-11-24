package com.ecommerce.backend.identidad.dto;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record PerfilResponse(
    UUID idUsuario,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    String correoElectronico,
    String telefono,
    String tipoDocumento,
    String numeroDocumento,
    String paisEmisorDocumento,
    Set<String> roles,
    OffsetDateTime fechaCreacion,
    boolean tieneContrasena
) {}
