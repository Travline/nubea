
package com.ecommerce.backend.catalogo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ColeccionRequest {

    @NotBlank(message = "El nombre de la colección es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @NotBlank(message = "El slug de la colección es obligatorio")
    @Size(max = 150, message = "El slug no puede superar los 150 caracteres")
    private String slug;

    public ColeccionRequest() {
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
}
