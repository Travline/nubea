package com.ecommerce.backend.inventario.controller;

import com.ecommerce.backend.inventario.dto.InventarioPage;
import com.ecommerce.backend.inventario.service.InventarioConsultaService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tiendas/{idTienda}/inventario")
@RequiredArgsConstructor
public class InventarioController {
    private final InventarioConsultaService service;

    @GetMapping
    @PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")
    public InventarioPage consultar(@PathVariable UUID idTienda,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String estadoProducto,
            @RequestParam(required = false) String estadoPresentacion,
            @RequestParam(required = false) String alertaStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.consultar(idTienda, texto, estadoProducto, estadoPresentacion, alertaStock, page, size);
    }
}
