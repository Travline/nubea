package com.ecommerce.backend.identidad.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.backend.identidad.dto.UsuarioResponse;
import com.ecommerce.backend.identidad.mapper.UsuarioMapper;
import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import java.util.UUID;
import com.ecommerce.backend.identidad.repository.UsuarioPlataformaRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class UsuarioService {

    private final UsuarioPlataformaRepository usuarioRepository;

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(UUID idUsuario){
        return usuarioRepository.findById(idUsuario)
               .map(UsuarioMapper::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("El Usuario no Existe"));
    }

    
}
