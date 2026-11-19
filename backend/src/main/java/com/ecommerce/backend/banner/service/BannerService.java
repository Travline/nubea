package com.ecommerce.backend.banner.service;

import com.ecommerce.backend.banner.dto.ActualizarBannerRequest;
import com.ecommerce.backend.banner.dto.BannerResponse;
import com.ecommerce.backend.banner.dto.ReordenarBannersRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface BannerService {
    List<BannerResponse> listarBanners(UUID idTienda);
    BannerResponse crearBanner(UUID idTienda, MultipartFile archivo, String enlaceDestino, Short orden);
    BannerResponse actualizarBanner(UUID idTienda, UUID idBanner, ActualizarBannerRequest request);
    BannerResponse sustituirImagen(UUID idTienda, UUID idBanner, MultipartFile archivo);
    List<BannerResponse> reordenarBanners(UUID idTienda, ReordenarBannersRequest request);
    void eliminarBanner(UUID idTienda, UUID idBanner);
}
