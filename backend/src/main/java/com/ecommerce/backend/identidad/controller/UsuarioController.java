package com.ecommerce.backend.identidad.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ecommerce.backend.identidad.dto.UsuarioResponse;
import com.ecommerce.backend.identidad.service.UsuarioService;
import java.util.UUID;

@RestController 
@RequestMapping ("/api/usuarios")
@RequiredArgsConstructor 
public class UsuarioController {
    private final UsuarioService usuarioService;

    @GetMapping("/{idUsuario}")
    public ResponseEntity<UsuarioResponse>obtener(@PathVariable UUID idUsuario){
        return ResponseEntity.ok(usuarioService.obtenerPorId(idUsuario));
    }
}
