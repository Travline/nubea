package com.ecommerce.backend.shared.exception;

/** 404 TIENDA_NO_DISPONIBLE: la tienda existe pero no está ACTIVA (borrador, suspendida o cerrada). */
public class TiendaNoDisponibleException extends RuntimeException {
    public TiendaNoDisponibleException() {
        super(MensajesUsuario.TIENDA_NO_DISPONIBLE);
    }
}
