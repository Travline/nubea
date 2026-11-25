package com.ecommerce.backend.cuentapago;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ecommerce.backend.compra.PagosConfiguracion;
import com.ecommerce.backend.cuentapago.repository.CuentaPagoConsultaRepository;
import com.ecommerce.backend.cuentapago.repository.CuentaPagoConsultaRepository.DatosCuentaPago;
import com.ecommerce.backend.cuentapago.service.CuentaPagoConsultaService;
import com.ecommerce.backend.exception.CuentaPagoNoConfiguradaException;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;
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

    @Test
    void ambientePagoSaleDeLaConfiguracionYSiNoDelUltimoCobro() {
        OffsetDateTime fecha = OffsetDateTime.now();
        when(repository.consultar(tienda)).thenReturn(Optional.of(
                new DatosCuentaPago("MERCADO_PAGO", "ACTIVA", "123456789", fecha, fecha, "PRUEBA")));
        // Sin configuración de cobros: último ambiente usado.
        assertEquals("PRUEBA", service.consultar(tienda).ambientePago());
        // Con la tienda habilitada en la configuración: manda la configuración.
        var habilitada = new PagosConfiguracion(true, "https://nubea.test", tienda + ":PRODUCCION", false);
        assertEquals("PRODUCCION", new CuentaPagoConsultaService(repository, habilitada).consultar(tienda).ambientePago());
        // En modo global manda PRODUCCION incluso para una tienda antes configurada en PRUEBA.
        var global = new PagosConfiguracion(true, "https://nubea.test", tienda + ":PRUEBA", true);
        assertEquals("PRODUCCION", new CuentaPagoConsultaService(repository, global).consultar(tienda).ambientePago());
        // Cobros deshabilitados: vuelve al último ambiente usado.
        var deshabilitada = new PagosConfiguracion(false, "https://nubea.test", "", true);
        assertEquals("PRUEBA", new CuentaPagoConsultaService(repository, deshabilitada).consultar(tienda).ambientePago());
        assertThrows(ReglaNegocioException.class, () -> deshabilitada.ambiente(tienda));
        // Nunca cobró y no está habilitada: null.
        when(repository.consultar(tienda)).thenReturn(Optional.of(new DatosCuentaPago("MERCADO_PAGO", "PENDIENTE", null, null, fecha)));
        assertNull(service.consultar(tienda).ambientePago());
    }
}
