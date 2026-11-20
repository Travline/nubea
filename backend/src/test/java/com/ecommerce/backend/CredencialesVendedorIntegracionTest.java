package com.ecommerce.backend;

import com.ecommerce.backend.cuentapago.oauth.*;
import com.ecommerce.backend.cuentapago.vault.AesGcmCredentialVault;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class CredencialesVendedorIntegracionTest {
    @Autowired JdbcTemplate jdbc;
    UUID tienda;
    MercadoPagoOAuthPort oauth;
    AesGcmCredentialVault vault;
    CredencialesVendedorService servicio;
    @BeforeEach void preparar() {
        tienda=UUID.randomUUID(); UUID usuario=UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?,'Prueba',?)",usuario,usuario+"@example.test");
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')",tienda,usuario,"t-"+tienda);
        oauth=mock(MercadoPagoOAuthPort.class);
        vault=new AesGcmCredentialVault("k",Map.of("k",Base64.getEncoder().encodeToString(new byte[32])));
        String sobre=vault.cifrar("YWNjZXNz.cmVmcmVzaA."+Instant.now().minusSeconds(1),"mercadopago:"+tienda);
        jdbc.update("INSERT INTO cuenta_pago_tienda(id_tienda,estado,id_cuenta_externa,referencia_credencial,fecha_vinculacion) VALUES (?,'ACTIVA','123',?,now())",tienda,sobre);
        servicio=new CredencialesVendedorService(jdbc,vault,oauth);
    }
    @Test void renuevaAmbosTokensYNoRenuevaUnaCredencialVigente() {
        when(oauth.renovar("refresh")).thenReturn(new MercadoPagoOAuthPort.Credenciales("123","nuevo-access","nuevo-refresh",3600L));
        assertEquals("nuevo-access",servicio.obtener(tienda).access());
        assertEquals("nuevo-access",servicio.obtener(tienda).access());
        verify(oauth,times(1)).renovar("refresh");
        String sobre=jdbc.queryForObject("SELECT referencia_credencial FROM cuenta_pago_tienda WHERE id_tienda=?",String.class,tienda);
        assertTrue(vault.descifrar(sobre,"mercadopago:"+tienda).contains(Base64.getUrlEncoder().withoutPadding().encodeToString("nuevo-refresh".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }
    @Test void unSegundoTrabajadorNoPuedeRenovarElMismoRefresh() {
        when(oauth.renovar("refresh")).thenAnswer(invocation->{
            assertThrows(ReglaNegocioException.class,()->servicio.obtener(tienda));
            return new MercadoPagoOAuthPort.Credenciales("123","nuevo","nuevo-refresh",3600L);
        });
        servicio.obtener(tienda);
        verify(oauth,times(1)).renovar("refresh");
    }
    @Test void unaRespuestaAntiguaNoSobrescribeLaReconexion() {
        when(oauth.renovar("refresh")).thenAnswer(invocation->{
            jdbc.update("UPDATE cuenta_pago_tienda SET referencia_credencial='reconexion',renovacion_dueno=NULL WHERE id_tienda=?",tienda);
            return new MercadoPagoOAuthPort.Credenciales("123","viejo","viejo-refresh",3600L);
        });
        assertThrows(ReglaNegocioException.class,()->servicio.obtener(tienda));
        assertEquals("reconexion",jdbc.queryForObject("SELECT referencia_credencial FROM cuenta_pago_tienda WHERE id_tienda=?",String.class,tienda));
    }
    @Test void soloRevocacionDefinitivaExigeReconexion() {
        when(oauth.renovar("refresh")).thenThrow(new OAuthProveedorException());
        assertThrows(OAuthProveedorException.class,()->servicio.obtener(tienda));
        assertEquals("ACTIVA",jdbc.queryForObject("SELECT estado FROM cuenta_pago_tienda WHERE id_tienda=?",String.class,tienda));
        doThrow(new MercadoPagoOAuthPort.AutorizacionRevocada()).when(oauth).renovar("refresh");
        assertThrows(ReglaNegocioException.class,()->servicio.obtener(tienda));
        assertEquals("ERROR",jdbc.queryForObject("SELECT estado FROM cuenta_pago_tienda WHERE id_tienda=?",String.class,tienda));
    }
}
