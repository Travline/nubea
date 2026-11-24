package com.ecommerce.backend.identidad.google;

import com.ecommerce.backend.shared.exception.MensajesUsuario;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Verifica ID tokens de Google Identity Services (RN-M1-05).
 *
 * <ul>
 *   <li>Firma RS256 con las claves públicas de Google (JWKS en caché; se refresca ante un {@code kid} nuevo).
 *       Cualquier otro algoritmo (incluidos {@code none} y HS256) se rechaza.</li>
 *   <li>{@code iss} = accounts.google.com o https://accounts.google.com (se normaliza al segundo).</li>
 *   <li>{@code aud} = GOOGLE_CLIENT_ID; {@code exp} vigente (60 s de margen de reloj); {@code iat} presente.</li>
 *   <li>{@code email_verified} = true; si no, 422 con {@code campos.idToken}.</li>
 * </ul>
 * Sin secreto de cliente. Nunca registra el token ni sus claims.
 */
@Component
public class GoogleIdTokenVerifier {
    /** Emisor normalizado que se guarda en {@code usuario_identidad.issuer}. */
    public static final String ISSUER = "https://accounts.google.com";
    private static final Set<String> EMISORES = Set.of("accounts.google.com", ISSUER);

    private final String clientId;
    private final String jwkSetUri;
    private volatile JwtDecoder decoder;

    public GoogleIdTokenVerifier(@Value("${google.client-id:}") String clientId,
                                 @Value("${google.jwk-set-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwkSetUri) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.jwkSetUri = jwkSetUri;
    }

    public boolean disponible() {
        return !clientId.isEmpty();
    }

    public DatosGoogle verificar(String idToken) {
        if (!disponible()) throw new GoogleNoDisponibleException();
        Jwt jwt;
        try {
            jwt = decoder().decode(idToken);
        } catch (BadJwtException ex) {
            // Firma, algoritmo, formato o validaciones (iss/aud/exp/iat) incorrectos.
            throw new GoogleTokenInvalidoException();
        } catch (JwtException ex) {
            // Fallo al obtener las claves públicas de Google: no es culpa del usuario.
            throw new GoogleNoDisponibleException();
        }
        if (!correoVerificado(jwt.getClaims().get("email_verified"))) {
            throw new ReglaNegocioException("idToken", MensajesUsuario.GOOGLE_CORREO_NO_VERIFICADO);
        }
        String email = jwt.getClaimAsString("email");
        if (jwt.getSubject() == null || jwt.getSubject().isBlank() || email == null || email.isBlank()) {
            throw new GoogleTokenInvalidoException();
        }
        return new DatosGoogle(ISSUER, jwt.getSubject(), email.trim().toLowerCase(Locale.ROOT),
                jwt.getClaimAsString("given_name") != null ? jwt.getClaimAsString("given_name") : jwt.getClaimAsString("name"),
                jwt.getClaimAsString("family_name"));
    }

    private static boolean correoVerificado(Object valor) {
        return Boolean.TRUE.equals(valor) || "true".equals(valor);
    }

    private JwtDecoder decoder() {
        JwtDecoder actual = decoder;
        if (actual == null) {
            synchronized (this) {
                if (decoder == null) {
                    NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                            .jwsAlgorithm(SignatureAlgorithm.RS256)
                            .build();
                    nimbus.setClaimSetConverter(conversorQueMarcaIatAusente());
                    nimbus.setJwtValidator(validadores(clientId));
                    decoder = nimbus;
                }
                actual = decoder;
            }
        }
        return actual;
    }

    static OAuth2TokenValidator<Jwt> validadores(String clientId) {
        OAuth2TokenValidator<Jwt> emisor = jwt -> EMISORES.contains(jwt.getClaimAsString("iss"))
                ? OAuth2TokenValidatorResult.success() : fallo("iss");
        OAuth2TokenValidator<Jwt> audiencia = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success() : fallo("aud");
        // Spring completa "iat" si falta; el conversor marca su ausencia en el token original.
        OAuth2TokenValidator<Jwt> emitido = jwt -> !jwt.hasClaim(SIN_IAT) && jwt.getExpiresAt() != null
                ? OAuth2TokenValidatorResult.success() : fallo("iat/exp");
        return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ofSeconds(60)), emitido, emisor, audiencia);
    }

    private static final String SIN_IAT = "nubea_sin_iat";

    /** Convierte los claims como Spring por defecto, marcando si el token original no traía "iat". */
    private static org.springframework.core.convert.converter.Converter<java.util.Map<String, Object>, java.util.Map<String, Object>> conversorQueMarcaIatAusente() {
        var porDefecto = MappedJwtClaimSetConverter.withDefaults(java.util.Collections.emptyMap());
        return originales -> {
            java.util.Map<String, Object> convertidos = new java.util.HashMap<>(porDefecto.convert(originales));
            if (originales.get("iat") == null) convertidos.put(SIN_IAT, true);
            return convertidos;
        };
    }

    private static OAuth2TokenValidatorResult fallo(String claim) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Claim " + claim + " no válido", null));
    }
}
