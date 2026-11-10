package com.ecommerce.backend.tienda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "plantilla")
@Getter    
@NoArgsConstructor 
public class Plantilla {

    @Id
    @Column(name = "id_plantilla")
    private Short idPlantilla;

    @Column (nullable = false, length = 60, unique = true)
    private String codigo;

    @Column (nullable = false, length = 100)
    private String nombre;

    @Column(name = "preview_url", columnDefinition = "text")
    private String previewUrl;

    @Column (nullable=false)
    private boolean activa;
}
