
package com.ecommerce.backend.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PresentacionImagenId implements Serializable {

    @Column(name = "id_presentacion")
    private UUID idPresentacion;

    @Column(name = "id_imagen")
    private UUID idImagen;

    public PresentacionImagenId() {
    }

    public PresentacionImagenId(UUID idPresentacion, UUID idImagen) {
        this.idPresentacion = idPresentacion;
        this.idImagen = idImagen;
    }

    public UUID getIdPresentacion() {
        return idPresentacion;
    }

    public void setIdPresentacion(UUID idPresentacion) {
        this.idPresentacion = idPresentacion;
    }

    public UUID getIdImagen() {
        return idImagen;
    }

    public void setIdImagen(UUID idImagen) {
        this.idImagen = idImagen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PresentacionImagenId that)) return false;
        return Objects.equals(idPresentacion, that.idPresentacion)
                && Objects.equals(idImagen, that.idImagen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPresentacion, idImagen);
    }
}
