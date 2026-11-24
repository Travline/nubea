package com.ecommerce.backend.identidad.service;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.backend.identidad.dto.ActualizarPerfilRequest;
import com.ecommerce.backend.identidad.dto.CambiarContrasenaRequest;
import com.ecommerce.backend.identidad.dto.PerfilResponse;
import com.ecommerce.backend.identidad.model.ProveedorIdentidad;
import com.ecommerce.backend.identidad.model.UsuarioPlataforma;
import com.ecommerce.backend.identidad.repository.UsuarioIdentidadRepository;
import com.ecommerce.backend.identidad.repository.UsuarioPlataformaRepository;
import com.ecommerce.backend.shared.exception.ConflictoCampoException;
import com.ecommerce.backend.shared.exception.MensajesUsuario;
import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import com.ecommerce.backend.shared.exception.ValidacionCampoException;

import lombok.RequiredArgsConstructor;

/** Datos de la cuenta del usuario que tiene la sesión iniciada (nunca recibe el id desde el request). */
@Service
@RequiredArgsConstructor
public class PerfilService {
    private final UsuarioPlataformaRepository usuarios;
    private final UsuarioIdentidadRepository identidades;
    private final DocumentoIdentidad documento;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PerfilResponse obtener(UUID idUsuario) {
        return toResponse(buscar(idUsuario));
    }

    @Transactional
    public PerfilResponse actualizar(UUID idUsuario, ActualizarPerfilRequest req) {
        UsuarioPlataforma usuario = buscar(idUsuario);
        usuario.setNombres(req.nombres().trim());
        usuario.setApellidoPaterno(limpiar(req.apellidoPaterno()));
        usuario.setApellidoMaterno(limpiar(req.apellidoMaterno()));
        usuario.setTelefono(limpiar(req.telefono()));
        documento.aplicar(usuario, req.tipoDocumento(), req.numeroDocumento(), req.paisEmisorDocumento());
        usuarios.saveAndFlush(usuario); // un documento ya registrado → uq_usuario_documento → 409 con campos.numeroDocumento
        return toResponse(usuario);
    }

    /**
     * Cambia la contraseña de la identidad LOCAL. Una cuenta creada solo con Google no tiene contraseña (409).
     * El controlador cierra después las demás sesiones del usuario y rota la actual.
     */
    @Transactional
    public void cambiarContrasena(UUID idUsuario, CambiarContrasenaRequest req) {
        var identidad = identidades.findByUsuario_IdUsuarioAndProveedor(idUsuario, ProveedorIdentidad.LOCAL)
                .orElseThrow(() -> new ConflictoCampoException("contrasenaActual", MensajesUsuario.CUENTA_SIN_CONTRASENA));
        if (!passwordEncoder.matches(req.contrasenaActual(), identidad.getPasswordHash())) {
            throw new ValidacionCampoException("contrasenaActual", MensajesUsuario.CONTRASENA_ACTUAL_INCORRECTA);
        }
        if (passwordEncoder.matches(req.contrasenaNueva(), identidad.getPasswordHash())) {
            throw new ValidacionCampoException("contrasenaNueva", MensajesUsuario.CONTRASENA_NUEVA_IGUAL);
        }
        identidad.setPasswordHash(passwordEncoder.encode(req.contrasenaNueva()));
    }

    private UsuarioPlataforma buscar(UUID idUsuario) {
        return usuarios.findById(idUsuario).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private PerfilResponse toResponse(UsuarioPlataforma u) {
        return new PerfilResponse(
            u.getIdUsuario(), u.getNombres(), u.getApellidoPaterno(), u.getApellidoMaterno(),
            u.getEmail(), u.getTelefono(),
            u.getTipoDocumento() == null ? null : u.getTipoDocumento().getCodigo(),
            u.getNumeroDocumento(), u.getPaisEmisorDocumento(),
            u.getRoles().stream().map(r -> r.getNombre().name()).collect(Collectors.toSet()),
            u.getFechaCreacion(),
            identidades.existsByUsuario_IdUsuarioAndProveedor(u.getIdUsuario(), ProveedorIdentidad.LOCAL),
            identidades.existsByUsuario_IdUsuarioAndProveedor(u.getIdUsuario(), ProveedorIdentidad.GOOGLE));
    }

    private String limpiar(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
