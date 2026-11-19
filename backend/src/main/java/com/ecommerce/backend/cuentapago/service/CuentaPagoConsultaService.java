package com.ecommerce.backend.cuentapago.service;

import com.ecommerce.backend.cuentapago.dto.CuentaPagoResponse;
import com.ecommerce.backend.cuentapago.repository.CuentaPagoConsultaRepository;
import com.ecommerce.backend.exception.CuentaPagoNoConfiguradaException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CuentaPagoConsultaService {
    private final CuentaPagoConsultaRepository repository;

    @Transactional(readOnly = true)
    public CuentaPagoResponse consultar(UUID idTienda) {
        var cuenta = repository.consultar(idTienda).orElseThrow(CuentaPagoNoConfiguradaException::new);
        String identificador = cuenta.idCuentaExterna();
        String enmascarado = enmascarar(identificador);
        return new CuentaPagoResponse(cuenta.proveedor(), cuenta.estado(), enmascarado,
                cuenta.fechaVinculacion(), cuenta.fechaActualizacion());
    }
    private static String enmascarar(String identificador) {
        if (identificador == null) return null;
        int longitud = identificador.length();
        if (longitud <= 4) return "*".repeat(longitud);
        return "*".repeat(longitud - 4) + identificador.substring(longitud - 4);
    }
}
