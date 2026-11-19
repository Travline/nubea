
package com.ecommerce.backend.catalogo.dto.request;

import jakarta.validation.constraints.Min;


public class ImagenProductoRequest {

    @Min(value = 1, message = "El orden debe ser mayor o igual a 1")
    private Integer orden;

    private Boolean esPrincipal;

    public ImagenProductoRequest() {
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
