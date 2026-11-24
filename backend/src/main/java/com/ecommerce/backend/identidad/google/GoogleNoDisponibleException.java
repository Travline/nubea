package com.ecommerce.backend.identidad.google;

import com.ecommerce.backend.shared.exception.MensajesUsuario;

/** 503 GOOGLE_NO_DISPONIBLE: falta GOOGLE_CLIENT_ID o no se pudieron obtener las claves públicas de Google. */
public class GoogleNoDisponibleException extends RuntimeException {
    public GoogleNoDisponibleException() {
        super(MensajesUsuario.GOOGLE_NO_DISPONIBLE);
    }
}
