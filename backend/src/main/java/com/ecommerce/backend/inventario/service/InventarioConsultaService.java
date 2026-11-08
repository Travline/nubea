package com.ecommerce.backend.inventario.service;

import com.ecommerce.backend.inventario.dto.InventarioPage;
import com.ecommerce.backend.inventario.repository.InventarioConsultaRepository;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventarioConsultaService {
    private final InventarioConsultaRepository repository;

    @Transactional(readOnly = true)
    public InventarioPage consultar(UUID tienda, String texto, String producto, String presentacion, String alerta, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Paginación inválida");
        if (producto != null && !Set.of("BORRADOR", "PUBLICADO", "INACTIVO").contains(producto))
            throw new IllegalArgumentException("Estado de producto inválido");
        if (presentacion != null && !Set.of("ACTIVA", "INACTIVA").contains(presentacion))
            throw new IllegalArgumentException("Estado de presentación inválido");
        if (alerta != null && !alerta.equals("true") && !alerta.equals("false"))
            throw new IllegalArgumentException("Alerta de stock inválida");
        String busqueda = texto == null || texto.isBlank() ? null : texto.trim();
        return repository.consultar(tienda, busqueda, producto, presentacion, alerta == null ? null : Boolean.valueOf(alerta), page, size);
    }
}
