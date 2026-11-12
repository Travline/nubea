
package com.ecommerce.backend.catalogo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
    name = "presentacion_producto",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_sku_tienda",
            columnNames = {"id_tienda", "sku"}
        ),
        @UniqueConstraint(
            name = "uq_presentacion_combinacion",
            columnNames = {"id_producto", "firma_valores"}
        )
    }
)
public class PresentacionProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_presentacion")
    private UUID idPresentacion;

    @Column(name = "id_producto", nullable = false)
    private UUID idProducto;

    @Column(name = "id_tienda", nullable = false)
    private UUID idTienda;

    @Column(name = "sku", nullable = false, length = 80)
    private String sku;

    @Column(name = "precio_override", precision = 12, scale = 2)
    private BigDecimal precioOverride;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVA";

    @Column(name = "firma_valores", nullable = false, columnDefinition = "TEXT")
    private String firmaValores = "";

    public PresentacionProducto() {
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

    public UUID getIdTienda() {
        return idTienda;
    }

    public void setIdTienda(UUID idTienda) {
        this.idTienda = idTienda;
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

    public String getFirmaValores() {
        return firmaValores;
    }

    public void setFirmaValores(String firmaValores) {
        this.firmaValores = firmaValores;
    }
}
