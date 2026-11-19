
package com.ecommerce.backend.catalogo.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
    name = "variante",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_variante_tienda_nombre",
            columnNames = {"id_tienda", "nombre"}
        )
    }
)
public class Variante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_variante")
    private UUID idVariante;

    @Column(name = "id_tienda", nullable = false)
    private UUID idTienda;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    public Variante() {
    }

    public UUID getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(UUID idVariante) {
        this.idVariante = idVariante;
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
}
