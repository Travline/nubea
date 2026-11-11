package com.ecommerce.backend.tienda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import java.time.OffsetDateTime;


@Entity 
@Table (name = "configuracion_tienda")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
@Builder 
public class ConfiguracionTienda {

    @Id
    @Column(name = "id_tienda")
    private UUID idTienda;
    
    @MapsId 
    @OneToOne (fetch = FetchType.LAZY)
    @JoinColumn (name= "id_tienda")
    private Tienda tienda;

    @Column (name ="titulo_inicio", length = 160)
    private String tituloInicio;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "id_plantilla", nullable = false)
    private Plantilla plantilla;

    @Column(name = "descripcion_inicio", columnDefinition = "text")
    private String descripcionInicio;

    @Column (name = "mostrar_carrusel", nullable = false)
    @Builder.Default
    private boolean mostrarCarrusel = true;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    @PrePersist 
    @PreUpdate 
    protected void alGuardar() {
        this.fechaActualizacion = OffsetDateTime.now();
    }
}
