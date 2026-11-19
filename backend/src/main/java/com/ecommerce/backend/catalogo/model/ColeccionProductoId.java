
package com.ecommerce.backend.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ColeccionProductoId implements Serializable {

    @Column(name = "id_coleccion")
    private UUID idColeccion;

    @Column(name = "id_producto")
    private UUID idProducto;

    public ColeccionProductoId() {
    }

    public ColeccionProductoId(UUID idColeccion, UUID idProducto) {
        this.idColeccion = idColeccion;
        this.idProducto = idProducto;
    }

    public UUID getIdColeccion() {
        return idColeccion;
    }

    public void setIdColeccion(UUID idColeccion) {
        this.idColeccion = idColeccion;
    }

    public UUID getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(UUID idProducto) {
        this.idProducto = idProducto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColeccionProductoId that)) return false;
        return Objects.equals(idColeccion, that.idColeccion)
                && Objects.equals(idProducto, that.idProducto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idColeccion, idProducto);
    }
}
