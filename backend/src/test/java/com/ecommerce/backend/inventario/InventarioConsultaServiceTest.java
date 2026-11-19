package com.ecommerce.backend.inventario;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ecommerce.backend.inventario.repository.InventarioConsultaRepository;
import com.ecommerce.backend.inventario.service.InventarioConsultaService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventarioConsultaServiceTest {
    private final InventarioConsultaRepository repository = mock(InventarioConsultaRepository.class);
    private final InventarioConsultaService service = new InventarioConsultaService(repository);
    private final UUID tienda = UUID.randomUUID();

    @Test
    void normalizaTextoYTransmiteFiltros() {
        service.consultar(tienda, "  camisa  ", "PUBLICADO", "ACTIVA", "true", 1, 10);
        verify(repository).consultar(tienda, "camisa", "PUBLICADO", "ACTIVA", true, 1, 10);
    }

    @Test
    void textoVacioYFiltrosAusentes() {
        service.consultar(tienda, "  ", null, null, null, 0, 20);
        verify(repository).consultar(tienda, null, null, null, null, 0, 20);
    }

    @Test
    void rechazaParametrosInvalidosSinConsultar() {
        assertThrows(IllegalArgumentException.class, () -> service.consultar(tienda, null, null, null, null, -1, 20));
        assertThrows(IllegalArgumentException.class, () -> service.consultar(tienda, null, null, null, null, 0, 101));
        assertThrows(IllegalArgumentException.class, () -> service.consultar(tienda, null, null, null, null, Integer.MAX_VALUE, 2));
        assertThrows(IllegalArgumentException.class, () -> service.consultar(tienda, null, "ACTIVO", null, null, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> service.consultar(tienda, null, null, "PUBLICADO", null, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> service.consultar(tienda, null, null, null, "si", 0, 20));
        verifyNoInteractions(repository);
    }
}
