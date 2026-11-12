
package com.ecommerce.backend.catalogo.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public class PresentacionProductoResponse {

    private UUID idPresentacion;
    private UUID idProducto;
    private String sku;
    private BigDecimal precioOverride;
    private String estado;

    public PresentacionProductoResponse() {
    }

    public UUID getIdPresentacion() {
        return idPresentacion;
    }

    public void setIdPresentacion(UUID idPresentacion) {
        this.idPresentacion = idPresentacion;
    }

    public UUID getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(UUID idProducto) {
        this.idProducto = idProducto;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
