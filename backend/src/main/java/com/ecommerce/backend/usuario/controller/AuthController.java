package com.ecommerce.backend.usuario.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.backend.usuario.dto.LoginRequest;
import com.ecommerce.backend.usuario.dto.RegistroUsuarioRequest;
import com.ecommerce.backend.usuario.dto.UsuarioResponse;
import com.ecommerce.backend.usuario.service.UsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController 
@RequestMapping("/api/auth") 
@RequiredArgsConstructor 
public class AuthController {
    private final UsuarioService usuarioService;

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody @Valid RegistroUsuarioRequest request){
         return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> login(@RequestBody @Valid LoginRequest request) {        
        return ResponseEntity.ok(usuarioService.login(request));
    }
    

}
    

