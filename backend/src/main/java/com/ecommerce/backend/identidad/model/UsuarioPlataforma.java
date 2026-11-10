package com.ecommerce.backend.identidad.model;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "usuario_plataforma")
@Getter 
@Setter 
@AllArgsConstructor
@NoArgsConstructor 
@Builder 
public class UsuarioPlataforma {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column (name = "id_usuario")
    private UUID idUsuario;

    @Column (nullable = false, length = 100)
    private String nombres;

    @Column (name = "apellido_paterno", length = 80)
    private String apellidoPaterno;

    @Column (name = "apellido_materno", length = 80)
    private String apellidoMaterno;

    @Column (nullable = false, length = 253, unique = true)
    private String email;

    @Column (length = 20)
    private String telefono;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn (name = "id_tipo_documento")
    private TipoDocumento tipoDocumento;

    @Column (name = "numero_documento", length = 30)
    private String numeroDocumento;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pais_emisor_documento", length = 2)
    private String paisEmisorDocumento;

    @Enumerated(EnumType.STRING)
    @Column (nullable = false, length = 20)
    @Builder.Default
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @CreationTimestamp 
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_cierre")
    private OffsetDateTime fechaCierre;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_rol",
        joinColumns = @JoinColumn(name = "id_usuario"),
        inverseJoinColumns = @JoinColumn(name = "id_rol")
    )
    @Builder.Default
    private Set<Rol> roles = new HashSet<>();

}
