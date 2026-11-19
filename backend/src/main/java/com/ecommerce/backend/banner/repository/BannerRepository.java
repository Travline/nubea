package com.ecommerce.backend.banner.repository;

import com.ecommerce.backend.banner.model.BannerTienda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BannerRepository extends JpaRepository<BannerTienda, UUID> {

    List<BannerTienda> findAllByIdTiendaOrderByOrdenAsc(UUID idTienda);

    Optional<BannerTienda> findByIdBannerAndIdTienda(UUID idBanner, UUID idTienda);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BannerTienda b where b.idBanner = :banner and b.idTienda = :tienda")
    Optional<BannerTienda> buscarConBloqueo(@Param("banner") UUID banner, @Param("tienda") UUID tienda);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BannerTienda b where b.idTienda = :tienda order by b.idBanner")
    List<BannerTienda> listarConBloqueo(@Param("tienda") UUID tienda);

    boolean existsByIdTiendaAndOrden(UUID idTienda, Short orden);

    long countByIdTienda(UUID idTienda);
}
