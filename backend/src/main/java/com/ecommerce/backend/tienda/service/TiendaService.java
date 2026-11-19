package com.ecommerce.backend.tienda.service;

import com.ecommerce.backend.identidad.model.EstadoUsuario;
import com.ecommerce.backend.identidad.model.NombreRol;
import com.ecommerce.backend.identidad.model.Rol;
import com.ecommerce.backend.identidad.model.UsuarioPlataforma;
import com.ecommerce.backend.identidad.repository.RolRepository;
import com.ecommerce.backend.identidad.repository.UsuarioPlataformaRepository;
import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import com.ecommerce.backend.shared.utils.SlugUtil;
import com.ecommerce.backend.tienda.dto.CrearTiendaRequest;
import com.ecommerce.backend.tienda.dto.TiendaResponse;
import com.ecommerce.backend.tienda.model.ConfiguracionTienda;
import com.ecommerce.backend.tienda.model.EstadoTienda;
import com.ecommerce.backend.exception.TiendaNoModificableException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import com.ecommerce.backend.tienda.model.Plantilla;
import com.ecommerce.backend.tienda.model.Tienda;
import com.ecommerce.backend.tienda.repository.ConfiguracionTiendaRepository;
import com.ecommerce.backend.tienda.repository.PlantillaRepository;
import com.ecommerce.backend.tienda.repository.TiendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import com.ecommerce.backend.shared.security.CurrentUserProvider;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TiendaService {

    private static final short PLANTILLA_POR_DEFECTO = 1;

    private final TiendaRepository tiendaRepository;
    private final ConfiguracionTiendaRepository configuracionRepository;
    private final PlantillaRepository plantillaRepository;
    private final UsuarioPlataformaRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final JdbcTemplate jdbcTemplate;
    private final CurrentUserProvider currentUserProvider;
    private final EntityManager entityManager;

    // ========== CREAR ==========
    @Transactional
    public TiendaResponse crearTienda(CrearTiendaRequest req) {
        UUID usuarioActual = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Usuario no autenticado"));
        if (req.idPropietario() != null && !usuarioActual.equals(req.idPropietario())) {
            throw new AccessDeniedException("No puedes crear tiendas para otro usuario");
        }
        jdbcTemplate.queryForList("SELECT id_usuario FROM usuario_plataforma WHERE id_usuario = ? FOR UPDATE", usuarioActual);
        UsuarioPlataforma propietario = usuarioRepository.findById(usuarioActual)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
        if (propietario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ReglaNegocioException("Tu cuenta no está activa, no puedes crear tiendas");
        }

        boolean esDueno = propietario.getRoles().stream()
                .anyMatch(r -> r.getNombre() == NombreRol.DUENO_TIENDA);
        boolean yaTieneTiendas = tiendaRepository.existsByPropietario_IdUsuario(propietario.getIdUsuario());

        if (!esDueno && yaTieneTiendas) {
            throw new ReglaNegocioException("No tienes permiso para crear tiendas nuevas");      // RN-M1-10
        }
        if (!esDueno) {
            Rol dueno = rolRepository.findByNombre(NombreRol.DUENO_TIENDA)
                    .orElseThrow(() -> new IllegalStateException("Falta el rol DUENO_TIENDA en la BD"));
            propietario.getRoles().add(dueno);                                                  // RN-M1-08
        }

        String base = SlugUtil.generar(req.nombre());                                           // RN-M1-12
        String slug = base;
        int i = 2;
        while (tiendaRepository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }

        Tienda tienda = Tienda.builder()
                .propietario(propietario)
                .nombre(req.nombre().trim())
                .slug(slug)
                .build();
        tiendaRepository.saveAndFlush(tienda);

        Plantilla plantilla = plantillaRepository.findById(PLANTILLA_POR_DEFECTO)              // RN-M3-02
                .orElseThrow(() -> new IllegalStateException("Falta la plantilla por defecto en la BD"));
        if (!plantilla.isActiva()) {
            throw new ReglaNegocioException("La plantilla por defecto está inactiva");
        }
        configuracionRepository.save(ConfiguracionTienda.builder()
                .tienda(tienda)
                .plantilla(plantilla)
                .tituloInicio(tienda.getNombre())
                .build());
        jdbcTemplate.update("INSERT INTO cuenta_pago_tienda (id_tienda) VALUES (?)", tienda.getIdTienda());

        return toResponse(tienda);
    }

    @Transactional
    public TiendaResponse activarTienda(UUID idTienda) {
        UUID usuarioActual = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Usuario no autenticado"));

        // El mismo orden que retirarDueno evita que una activación revierta una suspensión.
        jdbcTemplate.queryForList("SELECT id_usuario FROM usuario_plataforma WHERE id_usuario = ? FOR UPDATE", usuarioActual);
        Integer autorizados = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM usuario_plataforma u
                JOIN usuario_rol ur ON ur.id_usuario = u.id_usuario
                JOIN rol r ON r.id_rol = ur.id_rol
                WHERE u.id_usuario = ? AND u.estado = 'ACTIVO' AND r.nombre = 'DUENO_TIENDA'
                """, Integer.class, usuarioActual);
        if (autorizados == null || autorizados == 0) {
            throw new AccessDeniedException("No tienes permiso para activar tiendas");
        }

        Tienda tienda = tiendaRepository.findById(idTienda)
                .orElseThrow(() -> new RecursoNoEncontradoException("La tienda no existe"));
        entityManager.refresh(tienda, LockModeType.PESSIMISTIC_WRITE);
        if (!tienda.getPropietario().getIdUsuario().equals(usuarioActual)) {
            throw new AccessDeniedException("No puedes activar tiendas de otro usuario");
        }
        if (tienda.getEstado() == EstadoTienda.ACTIVA) return toResponse(tienda);
        if (tienda.getEstado() != EstadoTienda.BORRADOR) {
            throw new TiendaNoModificableException("Solo se puede activar una tienda en borrador");
        }
        tienda.setEstado(EstadoTienda.ACTIVA);
        return toResponse(tienda);
    }

    // ========== CONSULTAR ==========
    @Transactional(readOnly = true)
    public TiendaResponse obtenerPorSlug(String slug) {
        Tienda tienda = tiendaRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la tienda " + slug));
        return toResponse(tienda);
    }

    @Transactional(readOnly = true)
    public List<TiendaResponse> listarPorPropietario(UUID idUsuario) {
        if (!currentUserProvider.hasRole("ADMIN_GENERAL") && !currentUserProvider.getCurrentUserId().filter(idUsuario::equals).isPresent()) {
            throw new AccessDeniedException("No puedes consultar tiendas de otro usuario");
        }
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new RecursoNoEncontradoException("El usuario no existe");
        }
        return tiendaRepository.findByPropietario_IdUsuario(idUsuario).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<TiendaResponse> listarAdministracion(String texto, int page, int size) {
        var pagina = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("nombre").ascending().and(org.springframework.data.domain.Sort.by("idTienda")));
        return tiendaRepository.buscarAdministracion(texto == null ? "" : texto.trim(), pagina).map(this::toResponse);
    }

    // ========== APOYO ==========
    private TiendaResponse toResponse(Tienda t) {
        String plantilla = configuracionRepository.findById(t.getIdTienda())
                .map(c -> c.getPlantilla().getNombre())
                .orElse(null);
        return new TiendaResponse(
                t.getIdTienda(),
                t.getPropietario().getIdUsuario(),
                t.getNombre(),
                t.getSlug(),
                t.getEstado().name(),
                plantilla,
                t.getFechaCreacion());
    }
}
