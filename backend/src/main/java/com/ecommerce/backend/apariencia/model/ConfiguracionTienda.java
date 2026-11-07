package com.ecommerce.backend.apariencia.model;

import com.ecommerce.backend.plantilla.model.Plantilla;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "configuracion_tienda")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ConfiguracionTienda {

    @Id
    @Column(name = "id_tienda", nullable = false)
    private UUID idTienda;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_plantilla", nullable = false)
    private Plantilla plantilla;

    @Column(name = "titulo_inicio", length = 160)
    private String tituloInicio;

    @Column(name = "descripcion_inicio", columnDefinition = "TEXT")
    private String descripcionInicio;

    @Builder.Default
    @Column(name = "mostrar_carrusel", nullable = false)
    private Boolean mostrarCarrusel = Boolean.TRUE;

    @Builder.Default
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion = OffsetDateTime.now();
}
