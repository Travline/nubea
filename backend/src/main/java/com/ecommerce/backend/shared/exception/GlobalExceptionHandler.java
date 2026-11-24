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
public class GlobalExceptionHandler extends Modulo3ExceptionHandler {
    
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
      Map<String, String> campos = ex.getCampo() == null ? null : Map.of(ex.getCampo(), ex.getMessage());
      return ResponseEntity.status(HttpStatus.CONFLICT)
      .body(ErrorResponse.of(409,"DUPLICADO", ex.getMessage(), campos));
    }

    // 400 ligado a un campo concreto (más específico que el IllegalArgumentException de Modulo3ExceptionHandler)
    @ExceptionHandler(ValidacionCampoException.class)
    public ResponseEntity<ErrorResponse> validacionCampo(ValidacionCampoException ex) {
      return ResponseEntity.badRequest()
      .body(ErrorResponse.of(400, "DATOS_INVALIDOS", ex.getMessage(), Map.of(ex.getCampo(), ex.getMessage())));
    }

    @ExceptionHandler (RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse>noEncontrado(RecursoNoEncontradoException ex){
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse.of(404,"NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler (ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse>reglaNegocio(ReglaNegocioException ex){
      Map<String, String> campos = ex.getCampo() == null ? null : Map.of(ex.getCampo(), ex.getMessage());
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
      .body(ErrorResponse.of(422,"REGLA_NEGOCIO", ex.getMessage(), campos));
    }

    @ExceptionHandler(ConflictoCampoException.class)
    public ResponseEntity<ErrorResponse> conflictoCampo(ConflictoCampoException ex) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
      .body(ErrorResponse.of(409, "CONFLICTO", ex.getMessage(), Map.of(ex.getCampo(), ex.getMessage())));
    }

    @ExceptionHandler(com.ecommerce.backend.identidad.google.GoogleTokenInvalidoException.class)
    public ResponseEntity<ErrorResponse> googleTokenInvalido(RuntimeException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
      .body(ErrorResponse.of(401, "GOOGLE_TOKEN_INVALIDO", ex.getMessage()));
    }

    @ExceptionHandler(com.ecommerce.backend.identidad.google.GoogleNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> googleNoDisponible(RuntimeException ex) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
      .body(ErrorResponse.of(503, "GOOGLE_NO_DISPONIBLE", ex.getMessage()));
    }

    @ExceptionHandler(TiendaNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> tiendaNoDisponible(TiendaNoDisponibleException ex) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse.of(404, "TIENDA_NO_DISPONIBLE", ex.getMessage()));
    }

    // La BD rechazó algo (UNIQUE → 409, CHECK → 400). Es la "última barrera".
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex) {
        RestriccionesBd.Violacion violacion = RestriccionesBd.detectar(ex);
        var traduccion = RestriccionesBd.traducir(violacion.restriccion());
        if (traduccion.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of(409, "INTEGRIDAD", "Los datos no cumplen las reglas de la base de datos"));
        }
        var t = traduccion.get();
        Map<String, String> campos = Map.of(t.campo(), t.mensaje());
        if (violacion.tipo() == RestriccionesBd.Tipo.CHECK) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(400, "DATOS_INVALIDOS", t.mensaje(), campos));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(409, "DUPLICADO", t.mensaje(), campos));
    }
}
