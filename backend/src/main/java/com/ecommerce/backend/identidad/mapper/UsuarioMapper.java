package com.ecommerce.backend.identidad.mapper;

import java.util.stream.Collectors;

import com.ecommerce.backend.identidad.dto.UsuarioResponse;
import com.ecommerce.backend.identidad.model.UsuarioPlataforma;

public final class UsuarioMapper {
     private UsuarioMapper(){}

     public static UsuarioResponse toResponse(UsuarioPlataforma u){
            return new UsuarioResponse(
            u.getIdUsuario(), u.getNombres(),
            u.getApellidoPaterno(), u.getApellidoMaterno(),
            u.getEmail(), u.getEstado().name(),
            u.getRoles().stream().map(r -> r.getNombre().name()).collect(Collectors.toSet()),
            u.getFechaCreacion());
     }
    
}
