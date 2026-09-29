package com.nubea.spring.modules.categoria.dto;

import java.util.UUID;

public class CategoriaRequest {

    private UUID idTienda;
    private String nombre;
    private String descripcion;
    private Boolean activo;

    public CategoriaRequest() {
    }

    public UUID getIdTienda() {
        return idTienda;
    }

    public void setIdTienda(UUID idTienda) {
        this.idTienda = idTienda;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}