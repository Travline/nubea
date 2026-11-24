package com.ecommerce.backend.identidad.google;

import com.ecommerce.backend.shared.exception.MensajesUsuario;

/** 401 GOOGLE_TOKEN_INVALIDO: firma, emisor, audiencia o vigencia incorrectos. No indica qué falló. */
public class GoogleTokenInvalidoException extends RuntimeException {
    public GoogleTokenInvalidoException() {
        super(MensajesUsuario.GOOGLE_TOKEN_INVALIDO);
    }
}
