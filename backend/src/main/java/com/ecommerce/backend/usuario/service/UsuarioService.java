package com.ecommerce.backend.usuario.service;

import com.ecommerce.backend.usuario.dto.LoginRequest;
import com.ecommerce.backend.usuario.dto.RegistroUsuarioRequest;
import com.ecommerce.backend.usuario.dto.UsuarioResponse;

public interface UsuarioService {
    UsuarioResponse registrar(RegistroUsuarioRequest request);

    UsuarioResponse login(LoginRequest request);
}

