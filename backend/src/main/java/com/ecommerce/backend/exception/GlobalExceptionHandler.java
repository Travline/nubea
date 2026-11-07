package com.ecommerce.backend.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ecommerce.backend.shared.erros.ErrorResponse;

import java.util.stream.Collectors;

@RestControllerAdvice 
@Order (Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    // Usuarios duplicados 409 CONFLICT
    @ExceptionHandler(UsuarioYaExisteException.class)
    public ResponseEntity<ErrorResponse> manejarUsuarioYaExiste(
        UsuarioYaExisteException ex
    ){
        return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("USUARIO_YA_EXISTE", ex.getMessage()));
    }

    // Plantilla código duplicado 409 CONFLICT
    @ExceptionHandler(PlantillaCodigoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarPlantillaCodigoDuplicado(
        PlantillaCodigoDuplicadoException ex
    ){
        return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("PLANTILLA_CODIGO_DUPLICADO", ex.getMessage()));
    }

    // Plantilla no encontrada 404 NOT FOUND
    @ExceptionHandler(PlantillaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> manejarPlantillaNoEncontrada(
        PlantillaNoEncontradaException ex
    ){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse("PLANTILLA_NO_ENCONTRADA", ex.getMessage()));
    }

    // Argumento inválido / regla de validación de servicio 400 BAD REQUEST
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarIllegalArgument(
        IllegalArgumentException ex
    ){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("DATOS_INVALIDOS", ex.getMessage()));
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
        .body(new ErrorResponse("CUENTA_NO_DISPONIBLE", ex.getMessage()));
    }

    // Errores de validación de campos 400 BAD REQUEST
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(
        MethodArgumentNotValidException ex
    ){
        String detalle = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("DATOS_INVALIDOS", detalle));
    }

    // Acceso Denegado 403 FORBIDDEN
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(
        Exception ex
    ){
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("ACCESO_DENEGADO", "No tienes permisos para realizar esta acción"));
    }
}
