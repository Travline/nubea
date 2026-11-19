package com.ecommerce.backend.identidad.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "tipo_documento")
@NoArgsConstructor 
@Getter 
public class TipoDocumento {
    @Id 
    @Column (name = "id_tipo_documento")
    private Short idTipoDocumento;
    
    @Column(name = "codigo", nullable = false, length = 20, unique = true)
    private String codigo;

    @Column (name= "nombre", nullable = false, length = 100)
    private String nombre;

}
