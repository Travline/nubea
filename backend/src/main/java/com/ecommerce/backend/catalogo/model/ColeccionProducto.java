
package com.ecommerce.backend.catalogo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "coleccion_producto")
public class ColeccionProducto {

    @EmbeddedId
    private ColeccionProductoId id;

    public ColeccionProducto() {
    }

    public ColeccionProductoId getId() {
        return id;
    }

    public void setId(ColeccionProductoId id) {
        this.id = id;
    }
}
