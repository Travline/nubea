
package com.ecommerce.backend.catalogo.dto.response;

import java.util.UUID;

public class VarianteResponse {

    private UUID idVariante;
    private String nombre;

    public VarianteResponse() {
    }

    public UUID getIdVariante() {
        return idVariante;
    }

    public void setIdVariante(UUID idVariante) {
        this.idVariante = idVariante;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
