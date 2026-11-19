package com.ecommerce.backend.catalogo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PresentacionProductoRequest {

    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 80, message = "El SKU no puede superar los 80 caracteres")
    private String sku;

    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    private BigDecimal precioOverride;

    private List<UUID> idsValores = new ArrayList<>();

    public PresentacionProductoRequest() {
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrecioOverride() {
        return precioOverride;
    }

    public void setPrecioOverride(BigDecimal precioOverride) {
        this.precioOverride = precioOverride;
    }

    public List<UUID> getIdsValores() {
        return idsValores;
    }

    public void setIdsValores(List<UUID> idsValores) {
        this.idsValores = idsValores;
    }
}