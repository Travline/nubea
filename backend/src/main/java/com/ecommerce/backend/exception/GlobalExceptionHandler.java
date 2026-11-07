package com.ecommerce.backend.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ecommerce.backend.shared.erros.ErrorResponse;

@RestControllerAdvice 
@Order (Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    //Usuarios duplicados 409 CONFLICT
    @ExceptionHandler(UsuarioYaExisteException.class)
    public ResponseEntity< ErrorResponse> manejarUsuarioYaExiste(
        UsuarioYaExisteException ex
    ){
        return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("USUARIO_YA_EXISTE",ex.getMessage()
        ));
    }

    // Credenciales Incorrectas 401 UNAUTHORIZED
    @ExceptionHandler (CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> manejarCredencialesInvalidas(
         CredencialesInvalidasException ex
    ){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorResponse("CREDENCIALES_INVALIDAS", ex.getMessage()));
    }

    // Cuenta Inactiva 403 FORBIDDEN
    @ExceptionHandler(CuentaNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> manejarCuentaNoDisponible(
        CuentaNoDisponibleException ex
    ){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("CUENTA_NO_DISPONIBLE",ex.getMessage()));
    }
    
}
