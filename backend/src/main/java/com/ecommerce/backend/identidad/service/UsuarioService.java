package com.ecommerce.backend.identidad.service;

import com.ecommerce.backend.identidad.dto.UsuarioResponse;
import com.ecommerce.backend.identidad.model.NombreRol;
import com.ecommerce.backend.identidad.repository.RolRepository;
import com.ecommerce.backend.identidad.repository.UsuarioPlataformaRepository;
import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioPlataformaRepository usuarios;
    private final RolRepository roles;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(UUID id) {
        var u = usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return new UsuarioResponse(u.getIdUsuario(), u.getNombres(), u.getApellidoPaterno(), u.getApellidoMaterno(),
                u.getEmail(), u.getEstado().name(), u.getRoles().stream().map(r -> r.getNombre().name()).collect(Collectors.toSet()), u.getFechaCreacion());
    }

    @Transactional
    public void asignarRol(UUID id, NombreRol nombre) {
        jdbc.queryForList("SELECT id_usuario FROM usuario_plataforma WHERE id_usuario = ? FOR UPDATE", id);
        var u = usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        var rol = roles.findByNombre(nombre).orElseThrow(() -> new IllegalStateException("Rol no configurado"));
        u.getRoles().add(rol);
    }

    @Transactional
    public void retirarDueno(UUID id) {
        jdbc.queryForList("SELECT id_usuario FROM usuario_plataforma WHERE id_usuario = ? FOR UPDATE", id);
        var u = usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        u.getRoles().removeIf(r -> r.getNombre() == NombreRol.DUENO_TIENDA);
        jdbc.update("UPDATE tienda SET estado = 'SUSPENDIDA' WHERE id_propietario = ? AND estado IN ('ACTIVA', 'BORRADOR')", id);
    }
}
