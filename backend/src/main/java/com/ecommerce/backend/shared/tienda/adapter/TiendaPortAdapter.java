package com.ecommerce.backend.shared.tienda.adapter;

import com.ecommerce.backend.shared.tienda.TiendaInfo;
import com.ecommerce.backend.shared.tienda.TiendaPort;
import com.ecommerce.backend.shared.tienda.repository.TiendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TiendaPortAdapter implements TiendaPort {

    private final TiendaRepository tiendaRepository;

    @Override
    public Optional<TiendaInfo> obtenerTiendaPorId(UUID idTienda) {
        if (idTienda == null) {
            return Optional.empty();
        }
        return tiendaRepository.findById(idTienda)
                .map(t -> new TiendaInfo(t.getIdTienda(), t.getIdPropietario(), t.getEstado(), t.getNombre()));
    }
}
