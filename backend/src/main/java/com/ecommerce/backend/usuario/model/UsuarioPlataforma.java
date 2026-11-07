package com.ecommerce.backend.usuario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;

import com.ecommerce.backend.enums.EstadoUser;
import com.ecommerce.backend.enums.RolesUser;
import com.ecommerce.backend.shared.utils.GeneratedUuidV7;

@Entity 
@Table (name = "usuario_plataforma")
@Getter
@Setter 
@NoArgsConstructor (access = AccessLevel.PROTECTED)
@AllArgsConstructor 
@Builder  
public class UsuarioPlataforma {
    @Id 
    @GeneratedUuidV7 
    @Column (name ="id_usuario", updatable = false, nullable = false )
    private UUID userId;

    @Column (name = "nombres", nullable = false, length= 100)
    private String nombres;

    @Column (name = "apellidos", nullable = false, length= 100)
    private String apellidos;

    @Column (name = "correo", nullable = false, length= 150)
    private String correo;

    @Column (name = "contrasena", columnDefinition = "TEXT", nullable = false)
    private String contrasena;

    @Enumerated (EnumType.STRING)
    @Column (name = "rol", nullable = false, length= 20)
    private RolesUser rol;

    @Builder.Default
    @Enumerated (EnumType.STRING)
    @Column (name = "estado", nullable = false, length= 20)
    private EstadoUser estado = EstadoUser.ACTIVO;

    @Column (name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column (name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist 
    protected void onCreate(){
        LocalDateTime ahora = LocalDateTime.now();
        this.fechaCreacion = ahora;
        this.fechaActualizacion = ahora;

        if (this.estado == null) {
            this.estado = EstadoUser.ACTIVO;
        }
    }

    @PreUpdate 
    protected void onUpdate(){
        this.fechaActualizacion = LocalDateTime.now();
    }
}
