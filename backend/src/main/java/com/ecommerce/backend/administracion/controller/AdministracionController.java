package com.ecommerce.backend.administracion.controller;

import com.ecommerce.backend.identidad.dto.UsuarioResponse;
import com.ecommerce.backend.identidad.service.UsuarioService;
import com.ecommerce.backend.tienda.dto.TiendaAdministracionResponse;
import com.ecommerce.backend.tienda.service.TiendaService;
import lombok.RequiredArgsConstructor;
import com.ecommerce.backend.administracion.dto.PaginaAdministracion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN_GENERAL')")
@RequiredArgsConstructor
public class AdministracionController {
    private final UsuarioService usuarios;
    private final TiendaService tiendas;

    @GetMapping("/usuarios")
    public PaginaAdministracion<UsuarioResponse> usuarios(@RequestParam(defaultValue = "") String texto,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        validarPagina(page, size);
        return PaginaAdministracion.desde(usuarios.listarAdministracion(texto, page, size));
    }

    @GetMapping("/tiendas")
    public PaginaAdministracion<TiendaAdministracionResponse> tiendas(@RequestParam(defaultValue = "") String texto,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        validarPagina(page, size);
        return PaginaAdministracion.desde(tiendas.listarAdministracion(texto, page, size));
    }

    /** Una tienda con su dueño legible, para la vista de solo lectura del administrador. */
    @GetMapping("/tiendas/{idTienda}")
    public TiendaAdministracionResponse tienda(@PathVariable UUID idTienda) {
        return tiendas.obtenerAdministracion(idTienda);
    }

    /** Cuerpo de {@code PATCH /api/admin/tiendas/{id}/estado}: "SUSPENDIDA" o "ACTIVA" (reactivar → queda en BORRADOR). */
    public record CambioEstadoTienda(String estado) {}

    @PatchMapping("/tiendas/{idTienda}/estado")
    public TiendaAdministracionResponse cambiarEstadoTienda(@PathVariable UUID idTienda, @RequestBody CambioEstadoTienda cambio) {
        return tiendas.cambiarEstadoAdministracion(idTienda, cambio == null ? null : cambio.estado());
    }

    private void validarPagina(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Paginación inválida");
    }
}
