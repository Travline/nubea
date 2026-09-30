package com.ecommerce.backend.usuario.mapper;

import com.ecommerce.backend.usuario.model.UsuarioPlataforma;
import com.ecommerce.backend.usuario.dto.UsuarioResponse;

public class UsuarioMapper {
    
    private UsuarioMapper() {
        // Constructor privado para evitar la instanciación
    }

    // Método estático para mapear UsuarioPlataforma a UsuarioResponse
    public static UsuarioResponse toResponse(UsuarioPlataforma usuario)
    {
        return new UsuarioResponse(
            usuario.getUserId(),
            usuario.getNombres(),
            usuario.getApellidos(),
            usuario.getCorreo(),
            usuario.getRol(),
            usuario.getEstado()
        );
    }
}
