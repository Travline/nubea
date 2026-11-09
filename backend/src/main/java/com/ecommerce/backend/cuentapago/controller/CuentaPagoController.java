package com.ecommerce.backend.cuentapago.controller;

import com.ecommerce.backend.cuentapago.dto.CuentaPagoResponse;
import com.ecommerce.backend.cuentapago.service.CuentaPagoConsultaService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tiendas/{idTienda}/cuenta-pago")
@RequiredArgsConstructor
public class CuentaPagoController {
    private final CuentaPagoConsultaService service;
    private final com.ecommerce.backend.cuentapago.service.CuentaPagoDesconexionService desconexionService;

    @GetMapping
    @PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")
    public CuentaPagoResponse consultar(@PathVariable UUID idTienda) {
        return service.consultar(idTienda);
    }
    @org.springframework.web.bind.annotation.PostMapping("/desconexion")
    @PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")
    public org.springframework.http.ResponseEntity<Void> desconectar(@PathVariable UUID idTienda) {
        desconexionService.desconectar(idTienda);
        return org.springframework.http.ResponseEntity.noContent().build();
    }
}

