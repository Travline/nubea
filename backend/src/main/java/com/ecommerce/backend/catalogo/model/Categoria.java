
package com.ecommerce.backend.catalogo.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "categoria",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "categoria_id_tienda_slug_key",
            columnNames = {"id_tienda", "slug"}
        ),
        @UniqueConstraint(
            name = "uq_categoria_tienda",
            columnNames = {"id_categoria", "id_tienda"}
        )
    }
)
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_categoria", nullable = false)
    private UUID idCategoria;

    @Column(name = "id_tienda", nullable = false)
    private UUID idTienda;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    public Categoria() {
    }

    public UUID getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(UUID idCategoria) {
        this.idCategoria = idCategoria;
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
