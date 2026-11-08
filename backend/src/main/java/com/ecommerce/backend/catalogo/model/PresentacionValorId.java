
package com.ecommerce.backend.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PresentacionValorId implements Serializable {

    @Column(name = "id_presentacion")
    private UUID idPresentacion;

    @Column(name = "id_valor")
    private UUID idValor;

    public PresentacionValorId() {
    }

    public PresentacionValorId(UUID idPresentacion, UUID idValor) {
        this.idPresentacion = idPresentacion;
        this.idValor = idValor;
    }

    public UUID getIdPresentacion() {
        return idPresentacion;
    }

    public void setIdPresentacion(UUID idPresentacion) {
        this.idPresentacion = idPresentacion;
    }

    public UUID getIdValor() {
        return idValor;
    }

    public void setIdValor(UUID idValor) {
        this.idValor = idValor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PresentacionValorId that)) return false;
        return Objects.equals(idPresentacion, that.idPresentacion)
                && Objects.equals(idValor, that.idValor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPresentacion, idValor);
    }
}
