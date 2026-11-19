
package com.ecommerce.backend.catalogo.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
    name = "presentacion_valor",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_pv_una_por_variante",
            columnNames = {"id_presentacion", "id_variante"}
        )
    }
)
public class PresentacionValor {

    @EmbeddedId
    private PresentacionValorId id;

    @Column(name = "id_variante", nullable = false)
    private UUID idVariante;

    public PresentacionValor() {
    }

    public PresentacionValorId getId() {
        return id;
    }

    public void setId(PresentacionValorId id) {
        this.id = id;
    }

    public UUID getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(UUID idVariante) {
        this.idVariante = idVariante;
    }
}
