package com.ecommerce.backend.exception;

public class StorageException extends RuntimeException {
    public StorageException(String mensaje) {
        super(mensaje);
    }

    public StorageException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
