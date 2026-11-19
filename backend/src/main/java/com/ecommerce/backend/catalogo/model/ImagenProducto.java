
package com.ecommerce.backend.catalogo.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
    name = "imagen_producto"
)
public class ImagenProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_imagen")
    private UUID idImagen;

    @Column(name = "id_producto", nullable = false)
    private UUID idProducto;

    @Column(name = "storage_key", nullable = false, unique = true, columnDefinition = "TEXT")
    private String storageKey;

    @Column(name = "orden", nullable = false)
    private Integer orden = 1;

    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal = false;

    public ImagenProducto() {
    }

    public UUID getIdImagen() {
        return idImagen;
    }

    public void setIdImagen(UUID idImagen) {
        this.idImagen = idImagen;
    }

    public UUID getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(UUID idProducto) {
        this.idProducto = idProducto;
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
