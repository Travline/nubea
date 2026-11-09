
package com.ecommerce.backend.catalogo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class ImagenProductoRequest {

    @NotBlank(message = "La clave de almacenamiento es obligatoria")
    private String storageKey;

    @Min(value = 1, message = "El orden debe ser mayor o igual a 1")
    private Integer orden;

    private Boolean esPrincipal;

    public ImagenProductoRequest() {
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Boolean getEsPrincipal() {
        return esPrincipal;
    }

    public void setEsPrincipal(Boolean esPrincipal) {
        this.esPrincipal = esPrincipal;
    }
}
