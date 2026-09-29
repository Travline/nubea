package com.nubea.spring.modules.categoria.dto;

import java.util.UUID;

public class CategoriaResponse {

    private Long idCategoria;
    private UUID idTienda;
    private String nombre;
    private String descripcion;
    private Boolean activo;

    public CategoriaResponse() {
    }

    // Getters

    public Long getIdCategoria() {
        return idCategoria;
    }

    public UUID getIdTienda() {
        return idTienda;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Boolean getActivo() {
        return activo;
    }

    // Setters

    public void setIdCategoria(Long idCategoria) {
        this.idCategoria = idCategoria;
    }

    public void setIdTienda(UUID idTienda) {
        this.idTienda = idTienda;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}