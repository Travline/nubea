package com.ecommerce.backend.shared.exception;

import com.ecommerce.backend.exception.*;

import com.ecommerce.backend.cuentapago.oauth.OAuthConflictoException;
import com.ecommerce.backend.cuentapago.oauth.OAuthCuentaActivaException;
import com.ecommerce.backend.cuentapago.oauth.OAuthEstadoInvalidoException;
import com.ecommerce.backend.shared.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

public class Modulo3ExceptionHandler {
    @ExceptionHandler({PlantillaNoEncontradaException.class, BannerNoEncontradoException.class,
            ConfiguracionTiendaNoEncontradaException.class, CuentaPagoNoConfiguradaException.class})
    ResponseEntity<ErrorResponse> noEncontrado(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(404, "NO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler({PlantillaCodigoDuplicadoException.class, PlantillaInactivaException.class,
            TiendaNoModificableException.class, OAuthConflictoException.class, OAuthCuentaActivaException.class})
    ResponseEntity<ErrorResponse> conflicto(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(409, "CONFLICTO", ex.getMessage()));
    }

    @ExceptionHandler({ArchivoInvalidoException.class, ReordenInvalidoException.class,
            OAuthEstadoInvalidoException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> invalido(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(400, "DATOS_INVALIDOS", ex.getMessage()));
    }

    @ExceptionHandler(StorageException.class)
    ResponseEntity<ErrorResponse> almacenamiento(StorageException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(502, "ERROR_ALMACENAMIENTO", "Error de comunicación con el almacenamiento"));
    }
}
