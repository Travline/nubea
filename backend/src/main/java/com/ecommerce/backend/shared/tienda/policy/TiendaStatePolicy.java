package com.ecommerce.backend.shared.tienda.policy;

import com.ecommerce.backend.exception.TiendaNoModificableException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TiendaStatePolicy {

    private static final Set<String> ESTADOS_MODIFICABLES = Set.of("ACTIVA", "BORRADOR");

    public void validarModificable(String estado) {
        if (estado == null || !ESTADOS_MODIFICABLES.contains(estado.toUpperCase())) {
            throw new TiendaNoModificableException(
                "La tienda se encuentra en estado '" + estado + "' y no permite modificaciones de apariencia"
            );
        }
    }
}
