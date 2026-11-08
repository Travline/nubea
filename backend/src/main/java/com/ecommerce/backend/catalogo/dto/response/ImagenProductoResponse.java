
package com.ecommerce.backend.catalogo.dto.response;

import java.util.UUID;

public class ImagenProductoResponse {

    private UUID idImagen;
    private String storageKey;
    private Integer orden;
    private Boolean esPrincipal;

    public ImagenProductoResponse() {
    }

    public UUID getIdImagen() {
        return idImagen;
    }

    public void setIdImagen(UUID idImagen) {
        this.idImagen = idImagen;
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
