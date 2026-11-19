
package com.ecommerce.backend.catalogo.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
    name = "coleccion",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_coleccion_tienda_slug",
            columnNames = {"id_tienda", "slug"}
        )
    }
)
public class Coleccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_coleccion")
    private UUID idColeccion;

    @Column(name = "id_tienda", nullable = false)
    private UUID idTienda;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "slug", nullable = false, length = 150)
    private String slug;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    public Coleccion() {
    }

    public UUID getIdColeccion() {
        return idColeccion;
    }

    public void setIdColeccion(UUID idColeccion) {
        this.idColeccion = idColeccion;
    }

    public UUID getIdTienda() {
        return idTienda;
    }

    public void setIdTienda(UUID idTienda) {
        this.idTienda = idTienda;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }
}
