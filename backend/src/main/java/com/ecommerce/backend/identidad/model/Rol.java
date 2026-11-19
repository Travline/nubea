package com.ecommerce.backend.identidad.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "rol")
@Getter 
@NoArgsConstructor 
public class Rol {
    @Id 
    @Column (name = "id_rol")
    private Short idRol;

    @Enumerated (EnumType.STRING)
    @Column(nullable = false, length = 30, unique = true)
    private NombreRol nombre;
}