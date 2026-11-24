package com.ecommerce.backend.identidad.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.backend.identidad.dto.ActualizarPerfilRequest;
import com.ecommerce.backend.identidad.dto.CambiarContrasenaRequest;
import com.ecommerce.backend.identidad.dto.PerfilResponse;
import com.ecommerce.backend.identidad.service.PerfilService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Perfil del usuario con sesión iniciada (no recibe ids: siempre actúa sobre la propia cuenta). */
@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {
    private final PerfilService perfil;

    @GetMapping
    public ResponseEntity<PerfilResponse> obtener(Authentication authentication) {
        UUID id = usuario(authentication);
        return id == null ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() : ResponseEntity.ok(perfil.obtener(id));
    }

    @PutMapping
    public ResponseEntity<PerfilResponse> actualizar(Authentication authentication, @Valid @RequestBody ActualizarPerfilRequest req) {
        UUID id = usuario(authentication);
        return id == null ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() : ResponseEntity.ok(perfil.actualizar(id, req));
    }

    @PutMapping("/contrasena")
    public ResponseEntity<Void> cambiarContrasena(Authentication authentication, @Valid @RequestBody CambiarContrasenaRequest req) {
        UUID id = usuario(authentication);
        if (id == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        perfil.cambiarContrasena(id, req);
        return ResponseEntity.noContent().build();
    }

    private UUID usuario(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof UUID id ? id : null;
    }
}
