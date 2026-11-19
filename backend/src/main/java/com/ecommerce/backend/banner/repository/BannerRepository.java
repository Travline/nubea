package com.ecommerce.backend.banner.repository;

import com.ecommerce.backend.banner.model.BannerTienda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BannerRepository extends JpaRepository<BannerTienda, UUID> {

    List<BannerTienda> findAllByIdTiendaOrderByOrdenAsc(UUID idTienda);

    Optional<BannerTienda> findByIdBannerAndIdTienda(UUID idBanner, UUID idTienda);

    boolean existsByIdTiendaAndOrden(UUID idTienda, Short orden);

    long countByIdTienda(UUID idTienda);
}
