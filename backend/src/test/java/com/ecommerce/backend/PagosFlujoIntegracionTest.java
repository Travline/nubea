package com.ecommerce.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ecommerce.backend.compra.*;
import com.ecommerce.backend.cuentapago.oauth.CredencialesVendedorService;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties={"mercadopago.pagos.enabled=true","mercadopago.pagos.checkout-enabled=true",
    "mercadopago.pagos.delay-ms=3600000","mercadopago.pagos.tiendas=00000000-0000-0000-0000-000000000099:PRUEBA"})
@AutoConfigureMockMvc
@Transactional
class PagosFlujoIntegracionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired PagosStore store;
    @Autowired PagosProcesador procesador;
    @MockitoBean MercadoPagoOrders mercado;
    @MockitoBean CredencialesVendedorService tokens;
    final UUID tienda=UUID.fromString("00000000-0000-0000-0000-000000000099");
    UUID comprador,vendedor,direccion,presentacion;
    @BeforeEach void preparar() {
        comprador=UUID.randomUUID(); vendedor=UUID.randomUUID(); direccion=UUID.randomUUID(); presentacion=UUID.randomUUID();
        for(UUID u:List.of(comprador,vendedor)) jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?,'Prueba',?)",u,u+"@example.test");
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda','pago-test','ACTIVA')",tienda,vendedor);
        jdbc.update("INSERT INTO cuenta_pago_tienda(id_tienda,estado,id_cuenta_externa,referencia_credencial,fecha_vinculacion) VALUES (?,'ACTIVA','123456','cifrado',now())",tienda);
        UUID categoria=UUID.randomUUID(), producto=UUID.randomUUID(), carrito=UUID.randomUUID();
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Categoria','categoria')",categoria,tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base,estado) VALUES (?,?,?,'Producto',11.80,'PUBLICADO')",producto,categoria,tienda);
        jdbc.update("INSERT INTO presentacion_producto(id_presentacion,id_producto,id_tienda,sku) VALUES (?,?,?,'sku')",presentacion,producto,tienda);
        jdbc.update("INSERT INTO inventario(id_presentacion,stock_actual) VALUES (?,3)",presentacion);
        jdbc.update("INSERT INTO tienda_cliente(id_usuario,id_tienda) VALUES (?,?)",comprador,tienda);
        jdbc.update("INSERT INTO carrito(id_carrito,id_usuario,id_tienda) VALUES (?,?,?)",carrito,comprador,tienda);
        jdbc.update("INSERT INTO carrito_item(id_carrito,id_presentacion,cantidad) VALUES (?,?,1)",carrito,presentacion);
        jdbc.update("INSERT INTO ubigeo(codigo_ubigeo,departamento,provincia,distrito) VALUES ('999999','Prueba','Prueba','Prueba') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO direccion_cliente(id_direccion,id_usuario,codigo_ubigeo,direccion) VALUES (?,?,'999999','Calle')",direccion,comprador);
        when(tokens.obtener(tienda)).thenReturn(new CredencialesVendedorService.Token("123456","token-vendedor"));
        when(mercado.tiposExcluidos("token-vendedor")).thenReturn(List.of("ticket","account_money"));
        when(mercado.verificarCuenta("token-vendedor","123456")).thenReturn("PRUEBA");
        when(mercado.crear(eq("token-vendedor"),any(),any(),any(),any(),any(),any())).thenReturn(new MercadoPagoOrders.OrdenCreada("ORD123",URI.create("https://www.mercadopago.com.pe/checkout/v1/redirect?o=123")));
    }
    UUID reservar() { return store.preparar(comprador,"pago-test",direccion,"checkout_prueba_123456",new BigDecimal("11.80"),"123456","PRUEBA"); }
    MercadoPagoOrders.OrdenConsultada orden(UUID p,String estado,String detalle) {
        return new MercadoPagoOrders.OrdenConsultada("ORD123",p.toString(),"123456","PEN",new BigDecimal("11.80"),new BigDecimal("11.80"),estado,detalle);
    }
    int stock() { return jdbc.queryForObject("SELECT stock_actual FROM inventario WHERE id_presentacion=?",Integer.class,presentacion); }
    @Test void checkoutHttpRequiereSesionCsrfYConservaPedidoEnDobleEnvio() throws Exception {
        String ruta="/api/compra/tiendas/pago-test/checkout";
        String cuerpo="{\"idDireccion\":\""+direccion+"\",\"totalEsperado\":11.80,\"clave\":\"checkout_prueba_123456\"}";
        var sesion=UsernamePasswordAuthenticationToken.authenticated(comprador,null,List.of());
        mvc.perform(post(ruta).contentType("application/json").content(cuerpo)).andExpect(status().isForbidden());
        // Simular timeout: la API devuelve el pedido recuperable y nunca crea otro.
        when(mercado.crear(anyString(),any(),any(),any(),any(),any(),any())).thenThrow(new MercadoPagoOrders.OrdenInciertaException());
        for(int i=0;i<2;i++) mvc.perform(post(ruta).with(authentication(sesion)).with(csrf()).contentType("application/json").content(cuerpo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM pedido WHERE id_tienda=?",Integer.class,tienda));
        assertEquals(2,stock());
        var claves=org.mockito.ArgumentCaptor.forClass(UUID.class);
        verify(mercado,times(2)).crear(anyString(),claves.capture(),any(),any(),any(),any(),any());
        assertEquals(claves.getAllValues().get(0),claves.getAllValues().get(1));
    }
    @Test void confirmaUnaVezYReembolsaConReposicionUnica() {
        UUID p=reservar();
        when(mercado.consultar("token-vendedor","ORD123")).thenReturn(orden(p,"processed","accredited"));
        procesador.procesar(p); procesador.procesar(p);
        assertEquals("PAGADO",store.estado(p).estado()); assertEquals(2,stock());
        UUID clave=UUID.randomUUID();
        store.solicitarReembolso(tienda,p,vendedor,clave,true);
        store.solicitarReembolso(tienda,p,vendedor,clave,true);
        when(mercado.consultar("token-vendedor","ORD123")).thenReturn(orden(p,"processed","accredited"),orden(p,"refunded","refunded"));
        procesador.procesar(p); procesador.procesar(p);
        assertEquals("REEMBOLSADO",store.estado(p).estadoPago()); assertEquals(3,stock());
        verify(mercado,times(1)).reembolsar("token-vendedor","ORD123",clave);
    }
    @Test void cancelacionInciertaConservaStockYCobroTardioSeDevuelve() {
        UUID p=reservar();
        when(mercado.consultar(anyString(),anyString())).thenReturn(orden(p,"created","created"));
        procesador.procesar(p);
        jdbc.update("UPDATE pedido SET fecha_expiracion_reserva=now()-interval '1 minute' WHERE id_pedido=?",p);
        doThrow(new MercadoPagoOrders.OrdenInciertaException()).when(mercado).cancelar(anyString(),anyString(),any());
        procesador.procesar(p); assertEquals(2,stock()); assertEquals("PENDIENTE_PAGO",store.estado(p).estado());
        when(mercado.consultar(anyString(),anyString())).thenReturn(orden(p,"canceled","canceled"));
        procesador.procesar(p); assertEquals(3,stock());
        when(mercado.consultar(anyString(),anyString())).thenReturn(orden(p,"processed","accredited"),orden(p,"refunded","refunded"));
        procesador.procesar(p); assertEquals(3,stock()); assertEquals("REEMBOLSADO",store.estado(p).estadoPago());
    }
    @Test void ordenAjenaNoConfirmaYNoConsumeNotificacionNueva() {
        UUID p=reservar();
        when(mercado.consultar(anyString(),anyString())).thenReturn(new MercadoPagoOrders.OrdenConsultada("ORD123",p.toString(),"otra","PEN",new BigDecimal("11.80"),new BigDecimal("11.80"),"processed","accredited"));
        procesador.procesar(p); assertEquals("PENDIENTE_PAGO",store.estado(p).estado());
        jdbc.update("INSERT INTO notificacion_order_mercadopago(id_order,version) VALUES ('ORD123',2)");
        UUID dueno=UUID.randomUUID(); assertTrue(store.reclamar(p,dueno));
        assertFalse(store.reclamar(p,UUID.randomUUID()));
        store.terminar(p,dueno,"ORD123",1,null);
        assertEquals(1L,jdbc.queryForObject("SELECT version_procesada FROM notificacion_order_mercadopago WHERE id_order='ORD123'",Long.class));
    }
    @Test void cuentaReconectadaYAmbienteIncorrectoNoCobran() {
        UUID p=reservar();
        when(tokens.obtener(tienda)).thenReturn(new CredencialesVendedorService.Token("otro","otro-token"));
        procesador.procesar(p);
        verify(mercado,never()).crear(anyString(),any(),any(),any(),any(),any(),any());
        assertEquals("REQUIERE_REVISION",store.estado(p).incidencia());
    }
    @Test void recuperaNotificacionAnteriorALaRespuestaSinVolverACrear() {
        UUID p=reservar();
        jdbc.update("UPDATE intento_order_mercadopago SET estado='CREACION_INCIERTA' WHERE id_pedido=?",p);
        jdbc.update("INSERT INTO notificacion_order_mercadopago(id_order,receptor_sugerido) VALUES ('ORD123','123456')");
        when(mercado.consultar("token-vendedor","ORD123")).thenReturn(orden(p,"processed","accredited"));
        procesador.ejecutar();
        assertEquals("PAGADO",store.estado(p).estado());
        verify(mercado,never()).crear(anyString(),any(),any(),any(),any(),any(),any());
        assertEquals(2,stock());
    }
    @Test void reembolsoSinReposicionYAccesoAjenoRechazado() throws Exception {
        UUID p=reservar();
        when(mercado.consultar("token-vendedor","ORD123")).thenReturn(orden(p,"processed","accredited"));
        procesador.procesar(p);
        var compradorSesion=UsernamePasswordAuthenticationToken.authenticated(comprador,null,List.of());
        mvc.perform(post("/api/tiendas/"+tienda+"/pedidos/"+p+"/reembolsos")
            .with(authentication(compradorSesion)).with(csrf()).contentType("application/json")
            .content("{\"clave\":\""+UUID.randomUUID()+"\",\"reponerStock\":false}"))
            .andExpect(status().isForbidden());
        store.solicitarReembolso(tienda,p,vendedor,UUID.randomUUID(),false);
        when(mercado.consultar("token-vendedor","ORD123")).thenReturn(orden(p,"refunded","refunded"));
        procesador.procesar(p);
        assertEquals(2,stock()); assertEquals("REEMBOLSADO",store.estado(p).estadoPago());
    }
    @Test void dosSolicitudesConcurrentesCompartenReservaYClave() throws Exception {
        // Hacer visible el fixture a conexiones independientes; limpiar solo esta tienda al terminar.
        org.springframework.test.context.transaction.TestTransaction.flagForCommit();
        org.springframework.test.context.transaction.TestTransaction.end();
        try (var ejecutor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var inicio=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<UUID> compra=()->{ inicio.await(); return reservar(); };
            var a=ejecutor.submit(compra); var b=ejecutor.submit(compra); inicio.countDown();
            assertEquals(a.get(15,java.util.concurrent.TimeUnit.SECONDS),b.get(15,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(2,stock());
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM pedido WHERE id_tienda=?",Integer.class,tienda));
        } finally {
            jdbc.update("DELETE FROM reembolso_order WHERE id_pedido IN (SELECT id_pedido FROM pedido WHERE id_tienda=?)",tienda);
            jdbc.update("DELETE FROM checkout_idempotencia WHERE id_tienda=?",tienda);
            jdbc.update("DELETE FROM intento_order_mercadopago WHERE id_pedido IN (SELECT id_pedido FROM pedido WHERE id_tienda=?)",tienda);
            jdbc.update("DELETE FROM pago WHERE id_pedido IN (SELECT id_pedido FROM pedido WHERE id_tienda=?)",tienda);
            jdbc.update("DELETE FROM pedido_item WHERE id_pedido IN (SELECT id_pedido FROM pedido WHERE id_tienda=?)",tienda);
            jdbc.update("DELETE FROM pedido WHERE id_tienda=?",tienda);
            jdbc.update("DELETE FROM carrito_item WHERE id_carrito IN (SELECT id_carrito FROM carrito WHERE id_tienda=?)",tienda);
            jdbc.update("DELETE FROM carrito WHERE id_tienda=?",tienda);
            jdbc.update("DELETE FROM direccion_cliente WHERE id_usuario=?",comprador);
            jdbc.update("DELETE FROM tienda_cliente WHERE id_tienda=?",tienda);
            jdbc.update("DELETE FROM inventario WHERE id_presentacion=?",presentacion);
            jdbc.update("DELETE FROM presentacion_producto WHERE id_tienda=?",tienda);
            jdbc.update("DELETE FROM producto WHERE id_tienda=?",tienda);
            jdbc.update("DELETE FROM categoria WHERE id_tienda=?",tienda);
            jdbc.update("DELETE FROM cuenta_pago_tienda WHERE id_tienda=?",tienda);
            jdbc.update("DELETE FROM tienda WHERE id_tienda=?",tienda);
            jdbc.update("DELETE FROM usuario_plataforma WHERE id_usuario IN (?,?)",comprador,vendedor);
        }
    }
}
