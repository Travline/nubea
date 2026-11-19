package com.ecommerce.backend.identidad.model;

import java.time.OffsetDateTime;
import java.util.UUID;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "usuario_identidad")
@NoArgsConstructor 
@Getter
@Setter 
@AllArgsConstructor 
@Builder 
public class UsuarioIdentidad {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_identidad")
    private UUID idIdentidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioPlataforma usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProveedorIdentidad proveedor;

    @Column (length = 255)
    private String issuer;

    @Column(length = 255)
    private String subject;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "fecha_vinculacion", nullable = false, updatable = false)
    private OffsetDateTime fechaVinculacion;

    @PrePersist 
    protected void alCrear() {
        this.fechaVinculacion = OffsetDateTime.now();
    }

}
