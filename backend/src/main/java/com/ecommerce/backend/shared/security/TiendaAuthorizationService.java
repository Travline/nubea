package com.ecommerce.backend.shared.security;

import com.ecommerce.backend.exception.TiendaNoModificableException;
import com.ecommerce.backend.shared.exception.MensajesUsuario;
import com.ecommerce.backend.shared.tienda.policy.TiendaStatePolicy;
import com.ecommerce.backend.tienda.repository.TiendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Autorización por tienda para expresiones {@code @PreAuthorize}.
 *
 * <ul>
 *   <li>{@link #puedeVer}: lecturas del panel del vendedor. Basta con ser el propietario.</li>
 *   <li>{@link #puedeEditar}: escrituras. Exige ser el propietario, conservar el rol
 *       DUENO_TIENDA (RN-M1-10) y que la tienda esté en un estado modificable.</li>
 * </ul>
 *
 * Si no es propietario o no tiene el rol, devuelve {@code false} (403). Si cumple ambos
 * pero la tienda no es modificable (SUSPENDIDA/CERRADA), lanza
 * {@link TiendaNoModificableException} (409), el mismo código que el resto de la API.
 */
@Service("tiendaAuthorizationService")
@RequiredArgsConstructor
public class TiendaAuthorizationService {

    private static final String SQL_EDICION = """
            SELECT t.estado,
                   EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id_rol = ur.id_rol
                           WHERE ur.id_usuario = t.id_propietario AND r.nombre = 'DUENO_TIENDA') AS tiene_rol
            FROM tienda t
            WHERE t.id_tienda = ? AND t.id_propietario = ?
            """;

    private final TiendaRepository tiendaRepository;
    private final CurrentUserProvider currentUserProvider;
    private final JdbcTemplate jdbcTemplate;
    private final TiendaStatePolicy tiendaStatePolicy;

    public boolean puedeVer(UUID idTienda, Authentication authentication) {
        Optional<UUID> usuario = usuarioAutenticado(idTienda, authentication);
        if (usuario.isEmpty()) {
            return false;
        }
        return tiendaRepository.findById(idTienda)
                .map(tienda -> usuario.get().equals(tienda.getPropietario().getIdUsuario()))
                .orElse(false);
    }

    public boolean puedeEditar(UUID idTienda, Authentication authentication) {
        Optional<UUID> usuario = usuarioAutenticado(idTienda, authentication);
        if (usuario.isEmpty()) {
            return false;
        }
        List<EstadoEdicion> filas = jdbcTemplate.query(SQL_EDICION,
                (rs, n) -> new EstadoEdicion(rs.getString("estado"), rs.getBoolean("tiene_rol")),
                idTienda, usuario.get());
        if (filas.isEmpty() || !filas.get(0).tieneRol()) {
            return false;
        }
        String estado = filas.get(0).estado();
        if (!tiendaStatePolicy.esModificable(estado)) {
            throw new TiendaNoModificableException(MensajesUsuario.tiendaNoModificable(estado));
        }
        return true;
    }

    private Optional<UUID> usuarioAutenticado(UUID idTienda, Authentication authentication) {
        if (idTienda == null || authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return currentUserProvider.getCurrentUserId();
    }

    private record EstadoEdicion(String estado, boolean tieneRol) {
    }
}
