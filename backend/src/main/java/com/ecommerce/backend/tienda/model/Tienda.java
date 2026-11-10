package com.ecommerce.backend.tienda.model;

import com.ecommerce.backend.identidad.model.UsuarioPlataforma;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;

import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
@Entity 
@Table (name = "tienda")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class Tienda {

    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    @Column (name = "id_tienda")
    private UUID idTienda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_propietario", nullable = false)
    private UsuarioPlataforma propietario;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column (nullable = false, length = 150, unique=true)
    private String slug;

    @Enumerated (EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoTienda estado = EstadoTienda.BORRADOR;

    @CreationTimestamp
    @Column (name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column (name= "fecha_cierre")
    private OffsetDateTime fechaCierre;

}
