package com.ecommerce.backend.usuario.dto;

import com.ecommerce.backend.enums.EstadoUser;
import com.ecommerce.backend.enums.RolesUser;
import java.util.UUID;

public record UsuarioResponse(
    
    UUID idUsuario,
    String nombres,
    String apellidos,
    String correo,
    RolesUser rol,
    EstadoUser estado
) {}
