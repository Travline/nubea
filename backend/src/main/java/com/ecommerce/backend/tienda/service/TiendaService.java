package com.ecommerce.backend.tienda.service;

import com.ecommerce.backend.identidad.model.NombreRol;
import com.ecommerce.backend.identidad.model.EstadoUsuario;
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

@Service
@RequiredArgsConstructor
public class TiendaService {

    private static final short PLANTILLA_POR_DEFECTO = 1;   // "Minimalista", insertada por Flyway

    private final TiendaRepository tiendaRepository;
    private final ConfiguracionTiendaRepository configuracionRepository;
    private final PlantillaRepository plantillaRepository;
    private final UsuarioPlataformaRepository usuarioRepository;
    private final RolRepository rolRepository;

    @Transactional      // tienda + rol + configuración: todo o nada
    public TiendaResponse crearTienda(CrearTiendaRequest req) {

        // 1. El propietario existe y está activo
        UsuarioPlataforma propietario = usuarioRepository.findById(req.idPropietario())
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
        if (propietario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ReglaNegocioException("Tu cuenta no está activa, no puedes crear tiendas");
        }

        // 2. RN-M1-08 / RN-M1-10: rol de dueño
        boolean esDueno = propietario.getRoles().stream()
                .anyMatch(r -> r.getNombre() == NombreRol.DUENO_TIENDA);
        boolean yaTieneTiendas = tiendaRepository.existsByPropietario_IdUsuario(propietario.getIdUsuario());

        if (!esDueno && yaTieneTiendas) {
            // Tuvo tiendas pero un admin le quitó el rol (RN-M1-10)
            throw new ReglaNegocioException("No tienes permiso para crear tiendas nuevas");
        }
        if (!esDueno) {
            // Primera tienda: recibe el rol DUENO_TIENDA (RN-M1-08)
            Rol dueno = rolRepository.findByNombre(NombreRol.DUENO_TIENDA)
                    .orElseThrow(() -> new IllegalStateException("Falta el rol DUENO_TIENDA en la BD"));
            propietario.getRoles().add(dueno);
        }

        // 3. RN-M1-12: slug automático y único
        String base = SlugUtil.generar(req.nombre());
        String slug = base;
        int i = 2;
        while (tiendaRepository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }

        // 4. Guardar la tienda (estado BORRADOR por defecto)
        Tienda tienda = Tienda.builder()
                .propietario(propietario)
                .nombre(req.nombre().trim())
                .slug(slug)
                .build();
        tiendaRepository.save(tienda);

        // 5. RN-M3-02: configuración con la plantilla por defecto
        Plantilla plantilla = plantillaRepository.findById(PLANTILLA_POR_DEFECTO)
                .orElseThrow(() -> new IllegalStateException("Falta la plantilla por defecto en la BD"));
        ConfiguracionTienda config = ConfiguracionTienda.builder()
                .tienda(tienda)
                .plantilla(plantilla)
                .tituloInicio(tienda.getNombre())
                .build();
        configuracionRepository.save(config);

        return new TiendaResponse(
                tienda.getIdTienda(),
                propietario.getIdUsuario(),
                tienda.getNombre(),
                tienda.getSlug(),
                tienda.getEstado().name(),
                plantilla.getNombre(),
                tienda.getFechaCreacion());
    }
}
