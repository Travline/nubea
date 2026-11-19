package com.ecommerce.backend.cuentapago.oauth;

import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mercadopago.oauth", name = "enabled", havingValue = "true")
public class OAuthController {
    private final OAuthService service;
    public record InicioResponse(String urlAutorizacion) {
        @Override
        public String toString() { return "InicioResponse[urlAutorizacion=[REDACTADA]]"; }
    }

    @PostMapping("/api/tiendas/{idTienda}/cuenta-pago/mercado-pago/oauth/inicios")
    @PreAuthorize("@tiendaAuthorizationService.esPropietario(#idTienda, authentication)")
    public ResponseEntity<InicioResponse> iniciar(@PathVariable UUID idTienda) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(org.springframework.http.CacheControl.noStore()).body(new InicioResponse(service.iniciar(idTienda)));
    }
    @GetMapping("/api/integraciones/mercado-pago/oauth/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String state, @RequestParam(required = false) String code,
                                         @RequestParam(required = false) String error) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER).cacheControl(org.springframework.http.CacheControl.noStore()).location(URI.create(service.callback(state, code, error))).build();
    }
}
