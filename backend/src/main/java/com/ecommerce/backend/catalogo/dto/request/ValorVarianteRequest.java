
package com.ecommerce.backend.catalogo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ValorVarianteRequest {

    @NotBlank(message = "El valor de la variante es obligatorio")
    @Size(max = 100, message = "El valor no puede superar los 100 caracteres")
    private String valor;

    public ValorVarianteRequest() {
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
