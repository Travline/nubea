package com.ecommerce.backend.cuentapago.service;

import com.ecommerce.backend.exception.CuentaPagoNoConfiguradaException;
import com.ecommerce.backend.shared.security.CurrentUserProvider;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CuentaPagoDesconexionService {
    private final EntityManager em;
    private final CurrentUserProvider currentUser;
    @Transactional
    public void desconectar(UUID tienda) {
        UUID usuario = currentUser.getCurrentUserId().orElseThrow(() -> new AccessDeniedException("Acceso denegado"));
        var propietarios = em.createNativeQuery("SELECT id_propietario FROM tienda WHERE id_tienda=:id FOR UPDATE").setParameter("id", tienda).getResultList();
        if (propietarios.isEmpty() || !usuario.equals(propietarios.get(0))) throw new AccessDeniedException("Acceso denegado");
        var estados = em.createNativeQuery("SELECT estado FROM cuenta_pago_tienda WHERE id_tienda=:id FOR UPDATE").setParameter("id", tienda).getResultList();
        if (estados.isEmpty()) throw new CuentaPagoNoConfiguradaException();
        String anterior = (String) estados.get(0);
        em.createNativeQuery("UPDATE oauth_mercado_pago_intento SET estado='INVALIDADO', verifier_cifrado='' WHERE id_tienda=:id AND estado IN ('PENDIENTE','CONSUMIDO')").setParameter("id", tienda).executeUpdate();
        if ("DESCONECTADA".equals(anterior)) return;
        em.createNativeQuery("UPDATE cuenta_pago_tienda SET estado='DESCONECTADA', referencia_credencial=NULL, id_cuenta_externa=NULL, fecha_actualizacion=now() WHERE id_tienda=:id").setParameter("id", tienda).executeUpdate();
        em.createNativeQuery("INSERT INTO cuenta_pago_historial(id_tienda,id_usuario,estado_anterior,estado_nuevo,fecha_cambio) VALUES (:tienda,:usuario,:anterior,'DESCONECTADA',now())").setParameter("tienda", tienda).setParameter("usuario", usuario).setParameter("anterior", anterior).executeUpdate();
    }
}
