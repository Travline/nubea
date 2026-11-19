package com.ecommerce.backend.identidad.controller;

import com.ecommerce.backend.identidad.dto.UsuarioResponse;
import com.ecommerce.backend.identidad.model.NombreRol;
import com.ecommerce.backend.identidad.service.UsuarioService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarios;

    @GetMapping("/{idUsuario}")
    @PreAuthorize("hasRole('ADMIN_GENERAL') or principal == #idUsuario")
    public UsuarioResponse obtener(@PathVariable UUID idUsuario) { return usuarios.obtener(idUsuario); }

    @PutMapping("/{idUsuario}/roles/{rol}")
    @PreAuthorize("hasRole('ADMIN_GENERAL')")
    public ResponseEntity<Void> asignar(@PathVariable UUID idUsuario, @PathVariable NombreRol rol) {
        usuarios.asignarRol(idUsuario, rol);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{idUsuario}/roles/DUENO_TIENDA")
    @PreAuthorize("hasRole('ADMIN_GENERAL')")
    public ResponseEntity<Void> retirarDueno(@PathVariable UUID idUsuario) {
        usuarios.retirarDueno(idUsuario);
        return ResponseEntity.noContent().build();
    }
}
