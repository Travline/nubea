package com.ecommerce.backend.identidad.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.backend.identidad.dto.RegistroUsuarioRequest;
import com.ecommerce.backend.identidad.dto.UsuarioResponse;
import com.ecommerce.backend.identidad.service.AuthService;
import com.ecommerce.backend.identidad.dto.LoginRequest;
import com.ecommerce.backend.identidad.dto.GoogleLoginRequest;
import com.ecommerce.backend.identidad.google.DatosGoogle;
import com.ecommerce.backend.identidad.google.GoogleIdTokenVerifier;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import java.util.UUID;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController 
@RequestMapping ("/api/auth")
@RequiredArgsConstructor 
public class AuthController {
    private final AuthService authService;
    private final GoogleIdTokenVerifier google;

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) { return token; }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID id)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.usuarioActual(id));
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        try {
            iniciarSesion(authService.autenticar(req), Boolean.TRUE.equals(req.recordarme()), request, response);
            return ResponseEntity.noContent().build();
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Inicio de sesión con Google Identity Services (RN-M1-05). Exige CSRF como el login local.
     * 204 si la cuenta ya existía; 201 con el usuario si se creó; 409 si el correo ya tiene cuenta (no se vincula solo).
     * Usuario SUSPENDIDO o CERRADO: 401 sin cuerpo, igual que el login local.
     */
    @PostMapping("/google")
    public ResponseEntity<UsuarioResponse> google(@Valid @RequestBody GoogleLoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        DatosGoogle datos = google.verificar(req.idToken());
        AuthService.ResultadoGoogle resultado;
        try {
            resultado = autenticarConGoogle(datos);
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        iniciarSesion(resultado.autenticacion(), Boolean.TRUE.equals(req.recordarme()), request, response);
        return resultado.creado() == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.CREATED).body(resultado.creado());
    }

    /** Si dos primeros accesos con la misma cuenta de Google se cruzan, el segundo choca con una restricción: se reintenta una vez. */
    private AuthService.ResultadoGoogle autenticarConGoogle(DatosGoogle datos) {
        try {
            return authService.autenticarConGoogle(datos);
        } catch (DataIntegrityViolationException ex) {
            return authService.autenticarConGoogle(datos);
        }
    }

    /** Vincula Google a la cuenta con sesión iniciada (requiere sesión y CSRF). */
    @PostMapping("/google/vinculacion")
    public ResponseEntity<Void> vincularGoogle(@Valid @RequestBody GoogleLoginRequest req, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID id)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.vincularGoogle(id, google.verificar(req.idToken()));
        return ResponseEntity.noContent().build();
    }

    /** Misma sesión para el login local y el de Google: rota el id, 2 h o 24 h con "recordarme" y contexto de seguridad. */
    private void iniciarSesion(Authentication authentication, boolean recordar, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) request.changeSessionId();
        var session = request.getSession(true);
        session.setMaxInactiveInterval(recordar ? 86400 : 7200);
        session.setAttribute("recordarme", recordar);
        if (recordar) session.setAttribute("venceAbsoluto", System.currentTimeMillis() + 86400000L);
        else session.removeAttribute("venceAbsoluto");
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroUsuarioRequest req){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(req));
    }
    
}
