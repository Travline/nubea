package com.ecommerce.backend.catalogo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PresentacionProductoRequest {

    // Opcional al crear (se genera uno único por tienda); obligatorio al editar (lo valida el servicio).
    @Size(max = 80, message = "El código interno no puede superar los 80 caracteres")
    private String sku;

    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    private BigDecimal precioOverride;

    private List<UUID> idsValores = new ArrayList<>();

    // Opcionales, solo al crear: valores iniciales del registro de inventario.
    @Min(value = 0, message = "Las unidades no pueden ser negativas")
    private Integer stockInicial;

    @Min(value = 0, message = "Las unidades no pueden ser negativas")
    private Integer stockMinimo;

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

    public Integer getStockInicial() {
        return stockInicial;
    }

    public void setStockInicial(Integer stockInicial) {
        this.stockInicial = stockInicial;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }
}
