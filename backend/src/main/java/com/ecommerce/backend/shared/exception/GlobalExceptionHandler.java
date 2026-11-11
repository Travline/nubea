package com.ecommerce.backend.shared.exception;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    
  // 400: el JSON llegó mal escrito (falta una comilla, una coma...)
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException ex) {
    return ResponseEntity.badRequest()
            .body(ErrorResponse.of(400, "JSON_INVALIDO",
                    "El cuerpo de la petición no es un JSON válido. Revisa comillas, comas y llaves"));
}
  
  // 400: falló @NotBlank, @Email, @Pattern... del DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "VALIDACION", "Hay campos con errores", campos, OffsetDateTime.now()));
    }

    
    
    @ExceptionHandler (RecursosDuplicadosException.class)
    public ResponseEntity<ErrorResponse>duplicado(RecursosDuplicadosException ex){
      return ResponseEntity.status(HttpStatus.CONFLICT)
      .body(ErrorResponse.of(409,"DUPLICADO", ex.getMessage()));
    }

    @ExceptionHandler (RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse>noEncontrado(RecursoNoEncontradoException ex){
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse.of(404,"NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler (ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse>reglaNegocio(ReglaNegocioException ex){
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
      .body(ErrorResponse.of(422,"REGLA_NEGOCIO", ex.getMessage()));
    }

     // 409: la BD rechazó algo (UNIQUE, CHECK). Es la "última barrera"
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex) {
        String detalle = String.valueOf(ex.getMostSpecificCause().getMessage());
        String mensaje = "Los datos no cumplen las reglas de la base de datos";
        if (detalle.contains("usuario_plataforma_email_key")) mensaje = "Ya existe una cuenta con ese email";
        else if (detalle.contains("uq_usuario_documento"))    mensaje = "Ese documento ya está registrado";
        else if (detalle.contains("tienda_slug_key"))         mensaje = "Ese nombre de tienda ya está en uso";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "INTEGRIDAD", mensaje));
    }
}
