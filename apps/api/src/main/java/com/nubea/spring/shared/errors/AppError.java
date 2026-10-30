package com.nubea.spring.shared.errors;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import java.util.List;

@Getter
public class AppError extends RuntimeException {

    private final String name;
    private final HttpStatus httpCode;
    private final boolean isOperational;
    private final List<Object> details;

    public AppError(String name, HttpStatus httpCode, String description, boolean isOperational, List<Object> details) {
        super(description);
        this.name = name;
        this.httpCode = httpCode;
        this.isOperational = isOperational;
        this.details = details;
    }

    // Constructor sobrecargado para cuando no se requieran detalles
    public AppError(String name, HttpStatus httpCode, String description, boolean isOperational) {
        this(name, httpCode, description, isOperational, null);
    }
}