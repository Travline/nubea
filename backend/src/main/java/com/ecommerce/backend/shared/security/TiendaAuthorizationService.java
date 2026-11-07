package com.ecommerce.backend.shared.security;

import com.ecommerce.backend.shared.tienda.TiendaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service("tiendaAuthorizationService")
@RequiredArgsConstructor
public class TiendaAuthorizationService {

    private final TiendaPort tiendaPort;
    private final CurrentUserProvider currentUserProvider;

    public boolean esPropietario(UUID idTienda, Authentication authentication) {
        if (idTienda == null || authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Optional<UUID> currentUserIdOpt = currentUserProvider.getCurrentUserId();
        if (currentUserIdOpt.isEmpty()) {
            return false;
        }

        UUID currentUserId = currentUserIdOpt.get();
        return tiendaPort.obtenerTiendaPorId(idTienda)
                .map(tienda -> currentUserId.equals(tienda.idPropietario()))
                .orElse(false);
    }
}
