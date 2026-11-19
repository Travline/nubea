
package com.ecommerce.backend.catalogo.dto.response;

import java.util.UUID;

public class ColeccionResponse {

    private UUID idColeccion;
    private String nombre;
    private String slug;
    private Boolean activa;

    public ColeccionResponse() {
    }

    public UUID getIdColeccion() {
        return idColeccion;
    }

    public void setIdColeccion(UUID idColeccion) {
        this.idColeccion = idColeccion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }
}
