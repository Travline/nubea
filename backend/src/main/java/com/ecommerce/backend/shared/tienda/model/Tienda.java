package com.ecommerce.backend.shared.tienda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tienda")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Tienda {

    @Id
    @Column(name = "id_tienda", nullable = false)
    private UUID idTienda;

    @Column(name = "id_propietario", nullable = false)
    private UUID idPropietario;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "slug", nullable = false, unique = true, length = 150)
    private String slug;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;
}
