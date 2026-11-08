package com.ecommerce.backend.exception;

public class BannerNoEncontradoException extends RuntimeException {
    public BannerNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
