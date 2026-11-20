package com.ecommerce.backend.cuentapago.oauth;

import com.ecommerce.backend.cuentapago.vault.PaymentCredentialVault;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Lease persistente y escritura condicionada: nunca sobrescribir una reconexión. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix="mercadopago.oauth", name="enabled", havingValue="true")
public class CredencialesVendedorService {
    private final JdbcTemplate jdbc;
    private final PaymentCredentialVault vault;
    private final MercadoPagoOAuthPort oauth;
    private record Cuenta(String id, String sobre) {}
    public record Token(String cuenta, String access) {
        @Override public String toString() { return "Token[REDACTADO]"; }
    }
    public Token obtener(UUID tienda) { return obtener(tienda, false); }
    public Token obtener(UUID tienda, boolean forzar) {
        var cuentas = jdbc.query("SELECT id_cuenta_externa,referencia_credencial FROM cuenta_pago_tienda WHERE id_tienda=? AND estado='ACTIVA'",
                (r,n)->new Cuenta(r.getString(1),r.getString(2)),tienda);
        if (cuentas.size()!=1) throw new ReglaNegocioException("El vendedor debe reconectar Mercado Pago");
        var cuenta=cuentas.getFirst();
        String[] datos=vault.descifrar(cuenta.sobre(),"mercadopago:"+tienda).split("\\.",3);
        if (datos.length!=3) throw new ReglaNegocioException("Credenciales no válidas; reconecta Mercado Pago");
        if (!forzar && !datos[2].isBlank() && Instant.parse(datos[2]).isAfter(Instant.now().plusSeconds(300)))
            return new Token(cuenta.id(),decodificar(datos[0]));
        UUID dueno=UUID.randomUUID();
        if (jdbc.update("""
                UPDATE cuenta_pago_tienda SET renovacion_dueno=?,renovacion_hasta=now()+interval '60 seconds'
                WHERE id_tienda=? AND estado='ACTIVA' AND referencia_credencial=?
                  AND (renovacion_hasta IS NULL OR renovacion_hasta<now())
                """,dueno,tienda,cuenta.sobre())!=1)
            throw new ReglaNegocioException("Renovando autorización de pago; vuelve a intentar");
        try {
            var nueva=oauth.renovar(decodificar(datos[1]));
            if (!cuenta.id().equals(nueva.idCuenta()) || nueva.expiresIn()==null || nueva.expiresIn()<=300)
                throw new ReglaNegocioException("No se pudo verificar la renovación de Mercado Pago");
            String sobre=vault.cifrar(codificar(nueva.accessToken())+"."+codificar(nueva.refreshToken())+"."+Instant.now().plusSeconds(nueva.expiresIn()),"mercadopago:"+tienda);
            int guardadas=jdbc.update("""
                    UPDATE cuenta_pago_tienda SET referencia_credencial=?,renovacion_dueno=NULL,renovacion_hasta=NULL,fecha_actualizacion=now()
                    WHERE id_tienda=? AND estado='ACTIVA' AND referencia_credencial=? AND renovacion_dueno=?
                    """,sobre,tienda,cuenta.sobre(),dueno);
            if (guardadas!=1) throw new ReglaNegocioException("La vinculación cambió durante la renovación");
            return new Token(cuenta.id(),nueva.accessToken());
        } catch (MercadoPagoOAuthPort.AutorizacionRevocada ex) {
            jdbc.update("UPDATE cuenta_pago_tienda SET estado='ERROR',fecha_actualizacion=now() WHERE id_tienda=? AND referencia_credencial=? AND renovacion_dueno=?",tienda,cuenta.sobre(),dueno);
            throw new ReglaNegocioException("El vendedor debe reconectar Mercado Pago");
        } finally {
            jdbc.update("UPDATE cuenta_pago_tienda SET renovacion_dueno=NULL,renovacion_hasta=NULL WHERE id_tienda=? AND renovacion_dueno=?",tienda,dueno);
        }
    }
    @Scheduled(fixedDelayString="${mercadopago.oauth.renewal-delay-ms:60000}")
    public void renovarProximos() {
        var tiendas=jdbc.query("SELECT id_tienda FROM cuenta_pago_tienda WHERE estado='ACTIVA'",(r,n)->r.getObject(1,UUID.class));
        for (UUID tienda:tiendas) {
            try { obtener(tienda); } catch (RuntimeException ex) {
                // El panel muestra ERROR si la revocación es definitiva. Nunca registrar tokens o respuestas OAuth.
            }
        }
    }
    private static String decodificar(String s) { return new String(Base64.getUrlDecoder().decode(s),StandardCharsets.UTF_8); }
    private static String codificar(String s) { return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }
}
