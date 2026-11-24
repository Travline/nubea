package com.ecommerce.backend.shared.tienda.policy;

import com.ecommerce.backend.exception.TiendaNoModificableException;
import com.ecommerce.backend.shared.exception.MensajesUsuario;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TiendaStatePolicy {

    private static final Set<String> ESTADOS_MODIFICABLES = Set.of("ACTIVA", "BORRADOR");

    public boolean esModificable(String estado) {
        return estado != null && ESTADOS_MODIFICABLES.contains(estado.toUpperCase());
    }

    public void validarModificable(String estado) {
        if (!esModificable(estado)) {
            throw new TiendaNoModificableException(MensajesUsuario.tiendaNoModificable(estado));
        }
    }
}
