package com.ecommerce.backend.cuentapago.oauth;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mercadopago.oauth", name = "enabled", havingValue = "true")
public class OAuthTransactionStore {
    private final EntityManager em;
    public record Intento(UUID id, UUID tienda, UUID usuario, String verifier, String retorno) {
        @Override
        public String toString() { return "Intento[id=" + id + ", tienda=" + tienda + ", usuario=" + usuario + ", verifier=[REDACTADO]]"; }
    }

    @Transactional
    public void iniciar(UUID id, UUID tienda, UUID usuario, byte[] hash, String verifier, String retorno, int minutos) {
        bloquearTienda(tienda, usuario);
        var cuentas = em.createNativeQuery("SELECT estado FROM cuenta_pago_tienda WHERE id_tienda = :tienda")
                .setParameter("tienda", tienda).getResultList();
        if (!cuentas.isEmpty() && "ACTIVA".equals(cuentas.get(0))) throw new OAuthCuentaActivaException();
        em.createNativeQuery("INSERT INTO cuenta_pago_tienda(id_tienda) VALUES (:tienda) ON CONFLICT (id_tienda) DO NOTHING")
                .setParameter("tienda", tienda).executeUpdate();
        em.createNativeQuery("UPDATE oauth_mercado_pago_intento SET estado = 'INVALIDADO' WHERE id_tienda = :tienda AND estado IN ('PENDIENTE','CONSUMIDO')")
                .setParameter("tienda", tienda).executeUpdate();
        Instant ahora = Instant.now();
        em.createNativeQuery("""
                INSERT INTO oauth_mercado_pago_intento(id_intento,id_tienda,id_usuario,state_hash,verifier_cifrado,retorno_panel,fecha_creacion,fecha_expiracion,estado)
                VALUES (:id,:tienda,:usuario,:hash,:verifier,:retorno,:creacion,:expiracion,'PENDIENTE')
                """).setParameter("id", id).setParameter("tienda", tienda).setParameter("usuario", usuario)
                .setParameter("hash", hash).setParameter("verifier", verifier).setParameter("retorno", retorno)
                .setParameter("creacion", ahora).setParameter("expiracion", ahora.plusSeconds(minutos * 60L)).executeUpdate();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Intento consumir(byte[] hash) {
        var rows = em.createNativeQuery("""
                UPDATE oauth_mercado_pago_intento SET estado='CONSUMIDO'
                WHERE state_hash=:hash AND estado='PENDIENTE' AND fecha_expiracion > now()
                RETURNING id_intento,id_tienda,id_usuario,verifier_cifrado,retorno_panel
                """).setParameter("hash", hash).getResultList();
        if (rows.isEmpty()) throw new OAuthEstadoInvalidoException();
        Object[] row = (Object[]) rows.get(0);
        return new Intento((UUID) row[0], (UUID) row[1], (UUID) row[2], (String) row[3], (String) row[4]);
    }

    @Transactional(readOnly = true)
    public void validar(Intento intento) {
        var filas = em.createNativeQuery("SELECT 1 FROM tienda WHERE id_tienda=:tienda AND id_propietario=:usuario AND estado IN ('ACTIVA','BORRADOR')")
                .setParameter("tienda", intento.tienda()).setParameter("usuario", intento.usuario()).getResultList();
        if (filas.isEmpty()) throw new OAuthConflictoException("La tienda no permite la vinculación");
    }

    @Transactional
    public void activar(Intento intento, String cuenta, String cifrado) {
        bloquearTienda(intento.tienda(), intento.usuario());
        int vigente = em.createNativeQuery("UPDATE oauth_mercado_pago_intento SET estado='INVALIDADO', verifier_cifrado='' WHERE id_intento=:id AND estado='CONSUMIDO'")
                .setParameter("id", intento.id()).executeUpdate();
        if (vigente != 1) throw new OAuthConflictoException("El intento ya no está vigente");
        int actualizadas = em.createNativeQuery("""
                UPDATE cuenta_pago_tienda SET id_cuenta_externa=:cuenta, referencia_credencial=:credencial,
                    estado='ACTIVA', fecha_vinculacion=now(), fecha_actualizacion=now()
                WHERE id_tienda=:tienda AND estado <> 'ACTIVA'
                """).setParameter("cuenta", cuenta).setParameter("credencial", cifrado).setParameter("tienda", intento.tienda()).executeUpdate();
        if (actualizadas != 1) throw new OAuthCuentaActivaException();
    }

    private void bloquearTienda(UUID tienda, UUID usuario) {
        var filas = em.createNativeQuery("SELECT id_propietario, estado FROM tienda WHERE id_tienda=:id FOR UPDATE")
                .setParameter("id", tienda).getResultList();
        if (filas.isEmpty()) throw new OAuthConflictoException("La tienda no permite la vinculación");
        Object[] row = (Object[]) filas.get(0);
        if (!usuario.equals(row[0]) || !("ACTIVA".equals(row[1]) || "BORRADOR".equals(row[1])))
            throw new OAuthConflictoException("La tienda no permite la vinculación");
    }
}
