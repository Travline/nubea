
package com.ecommerce.backend.catalogo.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

public class PresentacionProductoResponse {

    private UUID idPresentacion;
    private UUID idProducto;
    private String sku;
    private BigDecimal precioOverride;
    private BigDecimal precioEfectivo;
    private String estado;
    private List<UUID> idsValores = List.of();

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

    public BigDecimal getPrecioEfectivo() { return precioEfectivo; }
    public void setPrecioEfectivo(BigDecimal precioEfectivo) { this.precioEfectivo = precioEfectivo; }

    public List<UUID> getIdsValores() { return idsValores; }
    public void setIdsValores(List<UUID> idsValores) { this.idsValores = idsValores; }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
