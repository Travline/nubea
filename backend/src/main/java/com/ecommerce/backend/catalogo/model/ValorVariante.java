
package com.ecommerce.backend.catalogo.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
    name = "valor_variante",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_valor_variante",
            columnNames = {"id_variante", "valor"}
        ),
        @UniqueConstraint(
            name = "uq_valor_con_variante",
            columnNames = {"id_valor", "id_variante"}
        )
    }
)
public class ValorVariante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_valor")
    private UUID idValor;

    @Column(name = "id_variante", nullable = false)
    private UUID idVariante;

    @Column(name = "valor", nullable = false, length = 100)
    private String valor;

    public ValorVariante() {
    }

    public UUID getIdValor() {
        return idValor;
    }

    public void setIdValor(UUID idValor) {
        this.idValor = idValor;
    }

    public UUID getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(UUID idVariante) {
        this.idVariante = idVariante;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
