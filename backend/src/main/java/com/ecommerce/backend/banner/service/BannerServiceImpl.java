package com.ecommerce.backend.banner.service;

import com.ecommerce.backend.banner.dto.ActualizarBannerRequest;
import com.ecommerce.backend.banner.dto.BannerResponse;
import com.ecommerce.backend.banner.dto.ReordenarBannersRequest;
import com.ecommerce.backend.banner.event.BannerStorageCleanupEvent;
import com.ecommerce.backend.banner.mapper.BannerMapper;
import com.ecommerce.backend.banner.model.BannerTienda;
import com.ecommerce.backend.banner.repository.BannerRepository;
import com.ecommerce.backend.config.BannerProperties;
import com.ecommerce.backend.exception.BannerNoEncontradoException;
import com.ecommerce.backend.exception.ReordenInvalidoException;
import com.ecommerce.backend.shared.storage.BannerStoragePort;
import com.ecommerce.backend.shared.storage.ImagenValidator;
import com.ecommerce.backend.shared.tienda.TiendaInfo;
import com.ecommerce.backend.shared.tienda.TiendaPort;
import com.ecommerce.backend.shared.tienda.policy.TiendaStatePolicy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    private static final Logger log = LoggerFactory.getLogger(BannerServiceImpl.class);

    private final BannerRepository bannerRepository;
    private final BannerStoragePort bannerStoragePort;
    private final ImagenValidator imagenValidator;
    private final BannerProperties bannerProperties;
    private final TiendaPort tiendaPort;
    private final TiendaStatePolicy tiendaStatePolicy;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> listarBanners(UUID idTienda) {
        return bannerRepository.findAllByIdTiendaOrderByOrdenAsc(idTienda)
                .stream()
                .map(BannerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BannerResponse crearBanner(UUID idTienda, MultipartFile archivo, String enlaceDestino, Short orden) {
        validarEstadoTienda(idTienda);

        if (orden == null || orden <= 0) {
            throw new IllegalArgumentException("El orden del banner debe ser un número entero positivo mayor a cero");
        }

        String enlaceNormalizado = normalizarYValidarEnlace(enlaceDestino);
        ImagenValidator.ImagenValidada imagen = imagenValidator.validarImagen(archivo);

        String storageKey = generarStorageKey(idTienda, imagen.extension());

        // Subir al almacenamiento externo antes de persistir
        try {
            bannerStoragePort.subir(storageKey, archivo.getInputStream(), archivo.getSize(), imagen.mimeType());
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el contenido del archivo de imagen para subirlo");
        }

        BannerTienda banner = BannerTienda.builder()
                .idTienda(idTienda)
                .storageKey(storageKey)
                .enlaceDestino(enlaceNormalizado)
                .orden(orden)
                .activo(Boolean.TRUE)
                .build();

        try {
            BannerTienda guardado = bannerRepository.saveAndFlush(banner);
            return BannerMapper.toResponse(guardado);
        } catch (Exception e) {
            log.error("Fallo la persistencia en base de datos tras subida de banner. Limpiando storage key: {}", storageKey);
            bannerStoragePort.eliminar(storageKey);
            throw e;
        }
    }

    @Override
    @Transactional
    public BannerResponse actualizarBanner(UUID idTienda, UUID idBanner, ActualizarBannerRequest request) {
        validarEstadoTienda(idTienda);

        BannerTienda banner = bannerRepository.findByIdBannerAndIdTienda(idBanner, idTienda)
                .orElseThrow(() -> new BannerNoEncontradoException(
                        "No se encontró el banner con id " + idBanner + " para la tienda indicada"));

        if (request.enlaceDestino() != null) {
            banner.setEnlaceDestino(normalizarYValidarEnlace(request.enlaceDestino()));
        }

        if (request.activo() != null) {
            banner.setActivo(request.activo());
        }

        BannerTienda guardado = bannerRepository.saveAndFlush(banner);
        return BannerMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public BannerResponse sustituirImagen(UUID idTienda, UUID idBanner, MultipartFile archivo) {
        validarEstadoTienda(idTienda);

        BannerTienda banner = bannerRepository.findByIdBannerAndIdTienda(idBanner, idTienda)
                .orElseThrow(() -> new BannerNoEncontradoException(
                        "No se encontró el banner con id " + idBanner + " para la tienda indicada"));

        ImagenValidator.ImagenValidada imagen = imagenValidator.validarImagen(archivo);
        String nuevoStorageKey = generarStorageKey(idTienda, imagen.extension());

        try {
            bannerStoragePort.subir(nuevoStorageKey, archivo.getInputStream(), archivo.getSize(), imagen.mimeType());
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el nuevo archivo de imagen para sustituirlo");
        }

        String antiguoStorageKey = banner.getStorageKey();
        banner.setStorageKey(nuevoStorageKey);

        try {
            BannerTienda guardado = bannerRepository.saveAndFlush(banner);
            // Publicar evento para eliminar el archivo antiguo solo tras commit
            eventPublisher.publishEvent(new BannerStorageCleanupEvent(antiguoStorageKey));
            return BannerMapper.toResponse(guardado);
        } catch (Exception e) {
            log.error("Fallo persistencia al sustituir imagen. Limpiando nuevo storage key: {}", nuevoStorageKey);
            bannerStoragePort.eliminar(nuevoStorageKey);
            throw e;
        }
    }

    @Override
    @Transactional
    public List<BannerResponse> reordenarBanners(UUID idTienda, ReordenarBannersRequest request) {
        validarEstadoTienda(idTienda);

        List<BannerTienda> actuales = bannerRepository.findAllByIdTiendaOrderByOrdenAsc(idTienda);
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
                .map(BannerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void eliminarBanner(UUID idTienda, UUID idBanner) {
        validarEstadoTienda(idTienda);

        BannerTienda banner = bannerRepository.findByIdBannerAndIdTienda(idBanner, idTienda)
                .orElseThrow(() -> new BannerNoEncontradoException(
                        "No se encontró el banner con id " + idBanner + " para la tienda indicada"));

        String storageKeyAEliminar = banner.getStorageKey();

        bannerRepository.delete(banner);
        bannerRepository.flush();

        // Limpieza de storage post-commit
        eventPublisher.publishEvent(new BannerStorageCleanupEvent(storageKeyAEliminar));
    }

    private void validarEstadoTienda(UUID idTienda) {
        TiendaInfo tienda = tiendaPort.obtenerTiendaPorId(idTienda)
                .orElseThrow(() -> new BannerNoEncontradoException("La tienda no existe o no se encuentra disponible"));
        tiendaStatePolicy.validarModificable(tienda.estado());
    }

    private String generarStorageKey(UUID idTienda, String extension) {
        return bannerProperties.getStoragePrefix() + idTienda + "/" + UUID.randomUUID() + extension;
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
