package com.ecommerce.backend.cuentapago;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ecommerce.backend.cuentapago.repository.CuentaPagoConsultaRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "CUENTA_PAGO_PG_TEST", matches = "true")
class CuentaPagoPostgresTest {
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired CuentaPagoConsultaRepository repository;

    private static UsernamePasswordAuthenticationToken autenticacion(UUID usuario) {
        return UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of());
    }

    @Test
    void consultaAutorizadaSinCredencialesYAislamiento() throws Exception {
        em.createNativeQuery("CREATE TEMP TABLE tienda (id_tienda uuid PRIMARY KEY, id_propietario uuid, nombre text, slug text, estado text) ON COMMIT DROP").executeUpdate();
        em.createNativeQuery("CREATE TEMP TABLE cuenta_pago_tienda (id_tienda uuid PRIMARY KEY, proveedor text, estado text, id_cuenta_externa text, referencia_credencial text, fecha_vinculacion timestamptz, fecha_actualizacion timestamptz) ON COMMIT DROP").executeUpdate();
        UUID propietario = UUID.randomUUID(), otro = UUID.randomUUID(), tienda = UUID.randomUUID(), tiendaSinCuenta = UUID.randomUUID(), ajena = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO tienda VALUES (:t,:p,'Uno','uno','ACTIVA'),(:s,:p,'Dos','dos','ACTIVA'),(:a,:o,'Tres','tres','ACTIVA')")
                .setParameter("t", tienda).setParameter("p", propietario).setParameter("s", tiendaSinCuenta).setParameter("a", ajena).setParameter("o", otro).executeUpdate();
        em.createNativeQuery("INSERT INTO cuenta_pago_tienda VALUES (:t,'MERCADO_PAGO','ACTIVA','123456789','secreto',now(),now()),(:a,'MERCADO_PAGO','PENDIENTE',NULL,NULL,NULL,now())")
                .setParameter("t", tienda).setParameter("a", ajena).executeUpdate();
        String ruta = "/api/tiendas/" + tienda + "/cuenta-pago";
        mvc.perform(get(ruta).with(authentication(autenticacion(propietario))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.proveedor").value("MERCADO_PAGO"))
                .andExpect(jsonPath("$.estado").value("ACTIVA")).andExpect(jsonPath("$.idCuentaExterna").value("*****6789"))
                .andExpect(jsonPath("$.fechaVinculacion").isNotEmpty()).andExpect(jsonPath("$.fechaActualizacion").isNotEmpty())
                .andExpect(jsonPath("$.referenciaCredencial").doesNotExist()).andExpect(jsonPath("$.accessToken").doesNotExist());
        mvc.perform(get(ruta).with(authentication(autenticacion(otro)))).andExpect(status().isForbidden());
        mvc.perform(get(ruta)).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/tiendas/" + tiendaSinCuenta + "/cuenta-pago").with(authentication(autenticacion(propietario))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("CUENTA_PAGO_NO_CONFIGURADA"));
        assertEquals("PENDIENTE", repository.consultar(ajena).orElseThrow().estado());
        assertNull(repository.consultar(ajena).orElseThrow().fechaVinculacion());
        assertEquals(2L, ((Number) em.createNativeQuery("SELECT count(*) FROM cuenta_pago_tienda").getSingleResult()).longValue());
        assertEquals("secreto", em.createNativeQuery("SELECT referencia_credencial FROM cuenta_pago_tienda WHERE id_tienda = :id").setParameter("id", tienda).getSingleResult());
    }
}
