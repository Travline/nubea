
package com.ecommerce.backend.catalogo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoriaRequest {

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    // Opcional: si llega vacío se genera desde el nombre (al crear) o se conserva (al editar).
    @Size(max = 120, message = "La dirección web no puede superar los 120 caracteres")
    private String slug;

    public CategoriaRequest() {
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
