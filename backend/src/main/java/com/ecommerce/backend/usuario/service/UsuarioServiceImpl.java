package com.ecommerce.backend.usuario.service;

import org.springframework.stereotype.Service;

import com.ecommerce.backend.enums.EstadoUser;
import com.ecommerce.backend.enums.RolesUser;
import com.ecommerce.backend.exception.CredencialesInvalidasException;
import com.ecommerce.backend.exception.CuentaNoDisponibleException;
import com.ecommerce.backend.exception.UsuarioYaExisteException;
import com.ecommerce.backend.usuario.dto.LoginRequest;
import com.ecommerce.backend.usuario.dto.RegistroUsuarioRequest;
import com.ecommerce.backend.usuario.repository.UsuarioRepository;
import com.ecommerce.backend.usuario.dto.UsuarioResponse;
import com.ecommerce.backend.usuario.mapper.UsuarioMapper;
import com.ecommerce.backend.usuario.model.UsuarioPlataforma;

import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override 
    public UsuarioResponse registrar (RegistroUsuarioRequest request){
           String correo = request.correo().trim().toLowerCase();

           if (usuarioRepository.existsByCorreoIgnoreCase(correo)){
                throw new UsuarioYaExisteException("El correo ya esta registrado");
           }

        UsuarioPlataforma usuario = UsuarioPlataforma.builder()
                                    .nombres(request.nombres().trim())
                                    .apellidos(request.apellidos().trim())
                                    .correo(correo)
                                    .contrasena(passwordEncoder.encode(request.contrasena()))
                                    .rol(RolesUser.VENDEDOR).build();
        UsuarioPlataforma guardado =
                 usuarioRepository.save(usuario);
        return UsuarioMapper.toResponse(guardado);
    }

    @Override 
    public UsuarioResponse login(LoginRequest request){
        String correo = request.correo().trim().toLowerCase();

        UsuarioPlataforma usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
        .orElseThrow(()-> new CredencialesInvalidasException("Credenciales Invalidas"));

        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasena()))
            {
                throw new CredencialesInvalidasException("Credenciales Invalidas");
            }

        if (usuario.getEstado() != EstadoUser.ACTIVO){
            throw new CuentaNoDisponibleException(
                "La cuenta no esta disponible"
            );
        }

        return UsuarioMapper.toResponse(usuario);

    }
    
}
