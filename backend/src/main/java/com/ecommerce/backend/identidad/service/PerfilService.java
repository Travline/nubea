package com.ecommerce.backend.identidad.service;

import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.backend.identidad.dto.ActualizarPerfilRequest;
import com.ecommerce.backend.identidad.dto.CambiarContrasenaRequest;
import com.ecommerce.backend.identidad.dto.PerfilResponse;
import com.ecommerce.backend.identidad.model.ProveedorIdentidad;
import com.ecommerce.backend.identidad.model.TipoDocumento;
import com.ecommerce.backend.identidad.model.UsuarioPlataforma;
import com.ecommerce.backend.identidad.repository.TipoDocumentoRepository;
import com.ecommerce.backend.identidad.repository.UsuarioIdentidadRepository;
import com.ecommerce.backend.identidad.repository.UsuarioPlataformaRepository;
import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;

import lombok.RequiredArgsConstructor;

/** Datos de la cuenta del usuario que tiene la sesión iniciada. */
@Service
@RequiredArgsConstructor
public class PerfilService {
    private final UsuarioPlataformaRepository usuarios;
    private final UsuarioIdentidadRepository identidades;
    private final TipoDocumentoRepository tiposDocumento;
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
        aplicarDocumento(usuario, req.tipoDocumento(), req.numeroDocumento(), req.paisEmisorDocumento());
        usuarios.saveAndFlush(usuario);
        return toResponse(usuario);
    }

    @Transactional
    public void cambiarContrasena(UUID idUsuario, CambiarContrasenaRequest req) {
        var identidad = identidades.findByUsuario_IdUsuarioAndProveedor(idUsuario, ProveedorIdentidad.LOCAL)
                .orElseThrow(() -> new ReglaNegocioException("Tu cuenta no usa contraseña para ingresar."));
        if (!passwordEncoder.matches(req.contrasenaActual(), identidad.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }
        if (passwordEncoder.matches(req.contrasenaNueva(), identidad.getPasswordHash())) {
            throw new ReglaNegocioException("La nueva contraseña debe ser distinta a la actual.");
        }
        identidad.setPasswordHash(passwordEncoder.encode(req.contrasenaNueva()));
    }

    private UsuarioPlataforma buscar(UUID idUsuario) {
        return usuarios.findById(idUsuario).orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    /** Mismas reglas que el registro: tipo, número y país van juntos (restricción chk_documento_conjunto). */
    private void aplicarDocumento(UsuarioPlataforma usuario, String tipoDocumento, String numeroDocumento, String paisEmisor) {
        String codigo = limpiar(tipoDocumento);
        String numero = limpiar(numeroDocumento);
        if (codigo == null && numero == null) {
            usuario.setTipoDocumento(null);
            usuario.setNumeroDocumento(null);
            usuario.setPaisEmisorDocumento(null);
            return;
        }
        if (codigo == null || numero == null) throw new ReglaNegocioException("El tipo y el número de documento van juntos.");
        TipoDocumento tipo = tiposDocumento.findByCodigo(codigo.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ReglaNegocioException("Tipo de documento no válido: " + codigo));

        String pais;
        switch (tipo.getCodigo()) {
            case "DNI" -> {
                if (!numero.matches("\\d{8}")) throw new ReglaNegocioException("El DNI debe tener 8 dígitos.");
                pais = "PE";
            }
            case "CE" -> {
                if (!numero.matches("[A-Za-z0-9]{9,12}")) throw new ReglaNegocioException("El carné de extranjería no es válido.");
                pais = "PE";
            }
            default -> {
                if (!numero.matches("[A-Za-z0-9]{6,12}")) throw new ReglaNegocioException("El pasaporte no es válido.");
                pais = limpiar(paisEmisor);
                if (pais == null) throw new ReglaNegocioException("Para pasaporte indica el país emisor (por ejemplo, CO).");
                pais = pais.toUpperCase(Locale.ROOT);
            }
        }
        usuario.setTipoDocumento(tipo);
        usuario.setNumeroDocumento(numero.toUpperCase(Locale.ROOT));
        usuario.setPaisEmisorDocumento(pais);
    }

    private PerfilResponse toResponse(UsuarioPlataforma u) {
        return new PerfilResponse(
            u.getIdUsuario(), u.getNombres(), u.getApellidoPaterno(), u.getApellidoMaterno(),
            u.getEmail(), u.getTelefono(),
            u.getTipoDocumento() == null ? null : u.getTipoDocumento().getCodigo(),
            u.getNumeroDocumento(), u.getPaisEmisorDocumento(),
            u.getRoles().stream().map(r -> r.getNombre().name()).collect(Collectors.toSet()),
            u.getFechaCreacion(),
            identidades.existsByUsuario_IdUsuarioAndProveedor(u.getIdUsuario(), ProveedorIdentidad.LOCAL));
    }

    private String limpiar(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
