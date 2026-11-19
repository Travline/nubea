package com.ecommerce.backend.banner.mapper;

import com.ecommerce.backend.banner.dto.BannerResponse;
import com.ecommerce.backend.banner.model.BannerTienda;

public final class BannerMapper {

    private BannerMapper() {
        // Constructor privado para evitar instanciación
    }

    public static BannerResponse toResponse(BannerTienda banner) {
        if (banner == null) {
            return null;
        }
        return new BannerResponse(
                banner.getIdBanner(),
                banner.getIdTienda(),
                banner.getStorageKey(),
                banner.getEnlaceDestino(),
                banner.getOrden(),
                banner.getActivo(),
                banner.getFechaCreacion());
    }
}
