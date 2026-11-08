
package com.ecommerce.backend.catalogo.dto.response;

import java.util.UUID;

public class ValorVarianteResponse {

    private UUID idValor;
    private String valor;

    public ValorVarianteResponse() {
    }

    public UUID getIdValor() {
        return idValor;
    }

    public void setIdValor(UUID idValor) {
        this.idValor = idValor;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
