package com.ecommerce.backend.cuentapago;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ecommerce.backend.cuentapago.repository.CuentaPagoConsultaRepository;
import com.ecommerce.backend.cuentapago.repository.CuentaPagoConsultaRepository.DatosCuentaPago;
import com.ecommerce.backend.cuentapago.service.CuentaPagoConsultaService;
import com.ecommerce.backend.exception.CuentaPagoNoConfiguradaException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CuentaPagoConsultaServiceTest {
    private final CuentaPagoConsultaRepository repository = mock(CuentaPagoConsultaRepository.class);
    private final CuentaPagoConsultaService service = new CuentaPagoConsultaService(repository);
    private final UUID tienda = UUID.randomUUID();

    @Test
    void protegeIdentificadoresYConservaEstadosYFechas() {
        OffsetDateTime fecha = OffsetDateTime.now();
        String[] estados = {"PENDIENTE", "ACTIVA", "DESCONECTADA", "ERROR"};
        String[] ids = {null, "1234", "12345", "123456789"};
        String[] esperados = {null, "****", "*2345", "*****6789"};
        for (int i = 0; i < estados.length; i++) {
            when(repository.consultar(tienda)).thenReturn(Optional.of(new DatosCuentaPago("MERCADO_PAGO", estados[i], ids[i], fecha, fecha)));
            var respuesta = service.consultar(tienda);
            assertEquals(estados[i], respuesta.estado());
            assertEquals(esperados[i], respuesta.idCuentaExterna());
            assertEquals(fecha, respuesta.fechaVinculacion());
            assertEquals(fecha, respuesta.fechaActualizacion());
        }
    }

    @Test
    void sinCuentaDevuelveErrorDeRecursoNoConfigurado() {
        when(repository.consultar(tienda)).thenReturn(Optional.empty());
        assertThrows(CuentaPagoNoConfiguradaException.class, () -> service.consultar(tienda));
    }
}
