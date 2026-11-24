package com.ecommerce.backend.identidad.dto;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/** {@code tieneContrasena}: tiene identidad LOCAL. {@code tieneGoogle}: tiene Google vinculado (para "Vincular Google"). */
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
    boolean tieneContrasena,
    boolean tieneGoogle
) {}
