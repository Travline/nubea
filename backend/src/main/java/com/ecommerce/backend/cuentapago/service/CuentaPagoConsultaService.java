package com.ecommerce.backend.cuentapago.service;

import com.ecommerce.backend.cuentapago.dto.CuentaPagoResponse;
import com.ecommerce.backend.cuentapago.repository.CuentaPagoConsultaRepository;
import com.ecommerce.backend.exception.CuentaPagoNoConfiguradaException;
import java.util.UUID;
import com.ecommerce.backend.compra.PagosConfiguracion;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuentaPagoConsultaService {
    private final CuentaPagoConsultaRepository repository;
    private final PagosConfiguracion configuracion;

    /** Sin configuración de cobros (pruebas unitarias): el ambiente sale solo del último cobro. */
    public CuentaPagoConsultaService(CuentaPagoConsultaRepository repository) {
        this(repository, null);
    }

    @Autowired
    public CuentaPagoConsultaService(CuentaPagoConsultaRepository repository, PagosConfiguracion configuracion) {
        this.repository = repository;
        this.configuracion = configuracion;
    }

    @Transactional(readOnly = true)
    public CuentaPagoResponse consultar(UUID idTienda) {
        var cuenta = repository.consultar(idTienda).orElseThrow(CuentaPagoNoConfiguradaException::new);
        String identificador = cuenta.idCuentaExterna();
        String enmascarado = enmascarar(identificador);
        String ambiente = Optional.ofNullable(configuracion)
                .flatMap(c -> c.ambienteHabilitado(idTienda))
                .orElse(cuenta.ambientePago());
        return new CuentaPagoResponse(cuenta.proveedor(), cuenta.estado(), enmascarado,
                cuenta.fechaVinculacion(), cuenta.fechaActualizacion(), ambiente);
    }
    private static String enmascarar(String identificador) {
        if (identificador == null) return null;
        int longitud = identificador.length();
        if (longitud <= 4) return "*".repeat(longitud);
        return "*".repeat(longitud - 4) + identificador.substring(longitud - 4);
    }
}
