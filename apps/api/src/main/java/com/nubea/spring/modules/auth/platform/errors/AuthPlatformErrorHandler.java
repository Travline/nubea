package com.nubea.spring.modules.auth.platform.errors;

import com.nubea.spring.shared.errors.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthPlatformErrorHandler {
  @ExceptionHandler(PlatformUserAlreadyExists.class)
  public ResponseEntity<ErrorResponse> handleUserExists(PlatformUserAlreadyExists ex) {
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("USER_ALREADY_EXISTS", ex.getMessage()));
  }

}
