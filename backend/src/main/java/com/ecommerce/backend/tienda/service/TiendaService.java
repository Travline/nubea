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
import com.ecommerce.backend.tienda.model.Plantilla;
import com.ecommerce.backend.tienda.model.Tienda;
import com.ecommerce.backend.tienda.repository.ConfiguracionTiendaRepository;
import com.ecommerce.backend.tienda.repository.PlantillaRepository;
import com.ecommerce.backend.tienda.repository.TiendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // ========== CREAR ==========
    @Transactional
    public TiendaResponse crearTienda(CrearTiendaRequest req) {
        UsuarioPlataforma propietario = usuarioRepository.findById(req.idPropietario())
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
        tiendaRepository.save(tienda);

        Plantilla plantilla = plantillaRepository.findById(PLANTILLA_POR_DEFECTO)              // RN-M3-02
                .orElseThrow(() -> new IllegalStateException("Falta la plantilla por defecto en la BD"));
        configuracionRepository.save(ConfiguracionTienda.builder()
                .tienda(tienda)
                .plantilla(plantilla)
                .tituloInicio(tienda.getNombre())
                .build());

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
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new RecursoNoEncontradoException("El usuario no existe");
        }
        return tiendaRepository.findByPropietario_IdUsuario(idUsuario).stream()
                .map(this::toResponse)
                .toList();
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