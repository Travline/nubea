package com.ecommerce.backend.shared.tienda;

import java.util.Optional;
import java.util.UUID;

public interface TiendaPort {
    Optional<TiendaInfo> obtenerTiendaPorId(UUID idTienda);
}
