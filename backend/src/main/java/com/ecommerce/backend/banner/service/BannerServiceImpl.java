package com.ecommerce.backend.banner.service;

import com.ecommerce.backend.banner.dto.ActualizarBannerRequest;
import com.ecommerce.backend.banner.dto.BannerResponse;
import com.ecommerce.backend.banner.dto.ReordenarBannersRequest;
import com.ecommerce.backend.banner.mapper.BannerMapper;
import com.ecommerce.backend.banner.model.BannerTienda;
import com.ecommerce.backend.banner.repository.BannerRepository;
import com.ecommerce.backend.config.BannerProperties;
import com.ecommerce.backend.exception.BannerNoEncontradoException;
import com.ecommerce.backend.exception.ReordenInvalidoException;
import com.ecommerce.backend.shared.storage.AlmacenamientoImagenes;
import com.ecommerce.backend.tienda.model.Tienda;
import com.ecommerce.backend.tienda.repository.TiendaRepository;
import com.ecommerce.backend.shared.tienda.policy.TiendaStatePolicy;
import com.ecommerce.backend.shared.security.CurrentUserProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;
    private final AlmacenamientoImagenes almacenamiento;
    private final BannerProperties bannerProperties;
    private final TiendaRepository tiendaRepository;
    private final TiendaStatePolicy tiendaStatePolicy;
    private final CurrentUserProvider currentUser;
    private final JdbcTemplate jdbc;

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> listarBanners(UUID idTienda) {
        return bannerRepository.findAllByIdTiendaOrderByOrdenAsc(idTienda)
                .stream()
                .map(this::respuesta)
                .toList();
    }

    @Override
    public BannerResponse crearBanner(UUID idTienda, MultipartFile archivo, String enlaceDestino, Short orden) {
        if (orden == null || orden <= 0) throw new IllegalArgumentException("El orden debe ser positivo");
        String enlace = normalizarYValidarEnlace(enlaceDestino);
        return almacenamiento.subirYGuardar(prefijoBanner(), idTienda, archivo, key -> {
            validarEstadoTienda(idTienda);
            BannerTienda banner = BannerTienda.builder().idTienda(idTienda).storageKey(key)
                    .enlaceDestino(enlace).orden(orden).activo(true).build();
            return respuesta(bannerRepository.saveAndFlush(banner));
        });
    }

    @Override
    @Transactional
    public BannerResponse actualizarBanner(UUID idTienda, UUID idBanner, ActualizarBannerRequest request) {
        validarEstadoTienda(idTienda);

        BannerTienda banner = bannerRepository.buscarConBloqueo(idBanner, idTienda)
                .orElseThrow(() -> new BannerNoEncontradoException(
                        "No se encontró el banner con id " + idBanner + " para la tienda indicada"));

        if (request.enlaceDestino() != null) {
            banner.setEnlaceDestino(normalizarYValidarEnlace(request.enlaceDestino()));
        }

        if (request.activo() != null) {
            banner.setActivo(request.activo());
        }

        BannerTienda guardado = bannerRepository.saveAndFlush(banner);
        return respuesta(guardado);
    }

    @Override
    public BannerResponse sustituirImagen(UUID idTienda, UUID idBanner, MultipartFile archivo) {
        return almacenamiento.subirYGuardar(prefijoBanner(), idTienda, archivo, key -> {
            validarEstadoTienda(idTienda);
            BannerTienda banner = bannerRepository.buscarConBloqueo(idBanner, idTienda)
                    .orElseThrow(() -> new BannerNoEncontradoException("Banner no encontrado"));
            String anterior = banner.getStorageKey();
            banner.setStorageKey(key);
            BannerResponse response = respuesta(bannerRepository.saveAndFlush(banner));
            almacenamiento.encolarEliminacion(anterior);
            return response;
        });
    }

    @Override
    @Transactional
    public List<BannerResponse> reordenarBanners(UUID idTienda, ReordenarBannersRequest request) {
        validarEstadoTienda(idTienda);

        List<BannerTienda> actuales = bannerRepository.listarConBloqueo(idTienda);
        if (actuales.isEmpty()) {
            throw new ReordenInvalidoException("La tienda no tiene banners configurados para reordenar");
        }

        List<ReordenarBannersRequest.ItemOrden> items = request.banners();
        if (items.size() != actuales.size()) {
            throw new ReordenInvalidoException(
                    "La solicitud debe contener exactamente la cantidad de banners de la tienda (" + actuales.size() + ")");
        }

        Set<UUID> idsRequest = items.stream()
                .map(ReordenarBannersRequest.ItemOrden::idBanner)
                .collect(Collectors.toSet());

        if (idsRequest.size() != items.size()) {
            throw new ReordenInvalidoException("La solicitud contiene identificadores de banners duplicados");
        }

        Set<UUID> idsActuales = actuales.stream()
                .map(BannerTienda::getIdBanner)
                .collect(Collectors.toSet());

        if (!idsRequest.equals(idsActuales)) {
            throw new ReordenInvalidoException(
                    "La solicitud de reordenamiento no coincide exactamente con los banners existentes de la tienda");
        }

        Set<Short> ordenesRequest = items.stream()
                .map(ReordenarBannersRequest.ItemOrden::orden)
                .collect(Collectors.toSet());

        if (ordenesRequest.size() != items.size()) {
            throw new ReordenInvalidoException("La solicitud contiene valores de orden duplicados");
        }

        Map<UUID, Short> mapaOrden = items.stream()
                .collect(Collectors.toMap(
                        ReordenarBannersRequest.ItemOrden::idBanner,
                        ReordenarBannersRequest.ItemOrden::orden
                ));

        for (BannerTienda banner : actuales) {
            banner.setOrden(mapaOrden.get(banner.getIdBanner()));
        }

        bannerRepository.saveAllAndFlush(actuales);

        actuales.sort(Comparator.comparing(BannerTienda::getOrden));
        return actuales.stream()
                .map(this::respuesta)
                .toList();
    }

    @Override
    @Transactional
    public void eliminarBanner(UUID idTienda, UUID idBanner) {
        validarEstadoTienda(idTienda);

        BannerTienda banner = bannerRepository.buscarConBloqueo(idBanner, idTienda)
                .orElseThrow(() -> new BannerNoEncontradoException(
                        "No se encontró el banner con id " + idBanner + " para la tienda indicada"));

        String storageKeyAEliminar = banner.getStorageKey();

        bannerRepository.delete(banner);
        bannerRepository.flush();

        // Limpieza de storage post-commit
        almacenamiento.encolarEliminacion(storageKeyAEliminar);
    }

    private void validarEstadoTienda(UUID idTienda) {
        var filas = jdbc.queryForList("SELECT id_propietario FROM tienda WHERE id_tienda=? FOR UPDATE", idTienda);
        if (filas.isEmpty() || currentUser.getCurrentUserId().filter(filas.get(0).get("id_propietario")::equals).isEmpty())
            throw new AccessDeniedException("Acceso denegado");
        Tienda tienda = tiendaRepository.findById(idTienda)
                .orElseThrow(() -> new BannerNoEncontradoException("La tienda no existe o no se encuentra disponible"));
        tiendaStatePolicy.validarModificable(tienda.getEstado().name());
    }

    private BannerResponse respuesta(BannerTienda banner) {
        return BannerMapper.toResponse(banner, almacenamiento.url(banner.getStorageKey()));
    }

    private String prefijoBanner() {
        return bannerProperties.getStoragePrefix().replaceAll("/+$", "");
    }

    private String normalizarYValidarEnlace(String enlace) {
        if (enlace == null || enlace.isBlank()) {
            return null;
        }
        String trimmed = enlace.trim();
        try {
            URI uri = URI.create(trimmed);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException("El enlace de destino debe ser una URL absoluta segura con protocolo HTTPS");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El enlace de destino no es una URL HTTPS válida: " + e.getMessage());
        }
        return trimmed;
    }
}
