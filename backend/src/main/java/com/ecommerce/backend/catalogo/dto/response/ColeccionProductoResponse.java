
package com.ecommerce.backend.catalogo.dto.response;

import java.util.UUID;

public class ColeccionProductoResponse {

    private UUID idColeccion;
    private UUID idProducto;

    public ColeccionProductoResponse() {
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
}
