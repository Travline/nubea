package com.ecommerce.backend.shared.exception;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
     int status, String error, String mensaje,  Map<String, String> campos, 
     OffsetDateTime timeStap) {
    public static ErrorResponse of(int status, String error, String mensaje){
        return new ErrorResponse(status,error,mensaje,null,OffsetDateTime.now());
        
    }    
} 
