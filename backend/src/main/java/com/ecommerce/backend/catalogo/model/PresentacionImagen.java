
package com.ecommerce.backend.catalogo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "presentacion_imagen")
public class PresentacionImagen {

    @EmbeddedId
    private PresentacionImagenId id;

    public PresentacionImagen() {
    }

    public PresentacionImagenId getId() {
        return id;
    }

    public void setId(PresentacionImagenId id) {
        this.id = id;
    }
}
