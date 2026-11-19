package com.ecommerce.backend.banner.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "banner_tienda")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BannerTienda {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_banner", nullable = false, updatable = false)
    private UUID idBanner;

    @Column(name = "id_tienda", nullable = false, updatable = false)
    private UUID idTienda;

    @Column(name = "storage_key", nullable = false, unique = true, columnDefinition = "TEXT")
    private String storageKey;

    @Column(name = "enlace_destino", columnDefinition = "TEXT")
    private String enlaceDestino;

    @Column(name = "orden", nullable = false)
    private Short orden;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Builder.Default
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();
}
