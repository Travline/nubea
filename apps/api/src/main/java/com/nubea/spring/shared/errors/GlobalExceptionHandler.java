package com.nubea.spring.shared.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(AppError.class)
    public ResponseEntity<?> handleAppError(AppError ex) {
        boolean isOperational = ex.isOperational();

        logger.error("[ErrorHandler] AppError capturado -> Name: {}, Message: {}, HttpCode: {}, IsOperational: {}, Details: {}",
                ex.getName(),
                ex.getMessage(),
                ex.getHttpCode(),
                isOperational,
                ex.getDetails(),
                ex
        );

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("code", ex.getName() != null ? ex.getName() : "INTERNAL_SERVER_ERROR");
        errorResponse.put("message", ex.getMessage());

        if (ex.getDetails() != null && !ex.getDetails().isEmpty()) {
            errorResponse.put("errors", ex.getDetails());
        }

        if (!isOperational) {
            logger.error("[Fatal] Error no operacional detectado. Se requiere revisión crítica del proceso.");
        }

        return ResponseEntity
                .status(ex.getHttpCode())
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex) {
        logger.error("[ErrorHandler] Error no controlado: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("code", "INTERNAL_SERVER_ERROR");
        errorResponse.put("message", "Ocurrió un error interno en el servidor");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {

        List<Object> validationDetails = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    Map<String, String> fieldErrorMap = new HashMap<>();
                    fieldErrorMap.put("field", error.getField());
                    fieldErrorMap.put("message", error.getDefaultMessage());
                    return fieldErrorMap;
                })
                .collect(Collectors.toList());

        AppError validationError = new AppError(
                "VALIDATION_ERROR",
                HttpStatus.BAD_REQUEST,
                "Los datos enviados en la petición no son válidos",
                true,
                validationDetails
        );

        return handleAppError(validationError);
    }
}