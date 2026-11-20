package com.ecommerce.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompraIntegracionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired com.ecommerce.backend.compra.CheckoutService checkout;
    private UsernamePasswordAuthenticationToken sesion(UUID usuario) { return UsernamePasswordAuthenticationToken.authenticated(usuario,null,List.of()); }
    private void crearUsuario(UUID id) { jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?,'Prueba',?)",id,id+"@example.test"); }
    @Test
    void carritoAislaTiendaYRecalculaSinReservarStock() throws Exception {
        UUID vendedor=UUID.randomUUID(), cliente=UUID.randomUUID(), otro=UUID.randomUUID(), tienda=UUID.randomUUID(), categoria=UUID.randomUUID(), producto=UUID.randomUUID(), presentacion=UUID.randomUUID();
        crearUsuario(vendedor); crearUsuario(cliente); crearUsuario(otro);
        String slug="t-"+tienda;
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')",tienda,vendedor,slug);
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Categoría','categoria')",categoria,tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base,estado) VALUES (?,?,?,'Producto',20,'PUBLICADO')",producto,categoria,tienda);
        jdbc.update("INSERT INTO presentacion_producto(id_presentacion,id_producto,id_tienda,sku) VALUES (?,?,?,'sku')",presentacion,producto,tienda);
        jdbc.update("INSERT INTO inventario(id_presentacion,stock_actual) VALUES (?,3)",presentacion);
        String ruta="/api/compra/tiendas/"+slug+"/carrito";
        mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        mvc.perform(put(ruta+"/items/"+presentacion).with(csrf()).with(authentication(sesion(cliente))).contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].cantidad").value(2)).andExpect(jsonPath("$.total").value(40));
        assertEquals(3,jdbc.queryForObject("SELECT stock_actual FROM inventario WHERE id_presentacion=?",Integer.class,presentacion));
        mvc.perform(get(ruta).with(authentication(sesion(otro)))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
        jdbc.update("UPDATE producto SET precio_base=30 WHERE id_producto=?",producto);
        mvc.perform(get(ruta).with(authentication(sesion(cliente)))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(60));
        jdbc.update("UPDATE categoria SET activa=false WHERE id_categoria=?",categoria);
        mvc.perform(put(ruta+"/items/"+presentacion).with(csrf()).with(authentication(sesion(cliente))).contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}"))
                .andExpect(status().isUnprocessableEntity());
    }
    @Test
    void direccionesPertenecenAlUsuarioYPrincipalEsUnica() throws Exception {
        UUID cliente=UUID.randomUUID(), otro=UUID.randomUUID(); crearUsuario(cliente); crearUsuario(otro);
        jdbc.update("INSERT INTO ubigeo(codigo_ubigeo,departamento,provincia,distrito) VALUES ('999999','Prueba','Prueba','Prueba') ON CONFLICT DO NOTHING");
        String cuerpo="{\"codigoUbigeo\":\"999999\",\"direccion\":\"Calle Uno\",\"principal\":true}";
        for (int i=0;i<2;i++) mvc.perform(post("/api/cliente/direcciones").with(csrf()).with(authentication(sesion(cliente))).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isOk());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM direccion_cliente WHERE id_usuario=? AND es_principal",Integer.class,cliente));
        UUID id=jdbc.queryForObject("SELECT id_direccion FROM direccion_cliente WHERE id_usuario=? AND es_principal",UUID.class,cliente);
        mvc.perform(delete("/api/cliente/direcciones/"+id).with(csrf()).with(authentication(sesion(otro)))).andExpect(status().isNotFound());
        mvc.perform(get("/api/cliente/direcciones").with(authentication(sesion(otro)))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
    @Test
    void impuestoEsSoloDeAdministradorYConservaLaTasaHistorica() throws Exception {
        UUID admin=UUID.randomUUID(), usuario=UUID.randomUUID(); crearUsuario(admin); crearUsuario(usuario);
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='ADMIN_GENERAL'", admin);
        var administrador=UsernamePasswordAuthenticationToken.authenticated(admin,null,List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN_GENERAL")));
        String inicio=jdbc.queryForObject("SELECT (CURRENT_DATE + 30)::text",String.class);
        String cuerpo="{\"tasa\":19,\"inicio\":\""+inicio+"\"}";
        mvc.perform(get("/api/admin/impuestos").with(authentication(sesion(usuario)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/impuestos").with(csrf()).with(authentication(administrador)).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/impuestos").with(authentication(administrador))).andExpect(status().isOk()).andExpect(jsonPath("$[0].tasa").value(19));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM configuracion_impuesto WHERE codigo='IGV_GENERAL' AND tasa_porcentaje=18 AND fecha_fin=?::date",Integer.class,inicio));
    }
    @Test
    void pedidosSoloSonVisiblesParaCompradorYPropietario() throws Exception {
        UUID vendedor=UUID.randomUUID(), cliente=UUID.randomUUID(), otro=UUID.randomUUID(), tienda=UUID.randomUUID(), pedido=UUID.randomUUID();
        crearUsuario(vendedor); crearUsuario(cliente); crearUsuario(otro); String slug="t-"+tienda;
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')",tienda,vendedor,slug);
        jdbc.update("INSERT INTO tienda_cliente(id_usuario,id_tienda) VALUES (?,?)",cliente,tienda);
        jdbc.update("""
          INSERT INTO pedido(id_pedido,id_usuario,id_tienda,id_impuesto,fecha_expiracion_reserva,subtotal,tasa_impuesto,monto_impuesto,total,
          nombre_receptor,direccion_entrega,ubigeo_entrega,departamento_entrega,provincia_entrega,distrito_entrega)
          VALUES (?,?,?,1,now()+interval '30 minutes',10,18,1.80,11.80,'Receptor','Calle','150101','Lima','Lima','Lima')
          """,pedido,cliente,tienda);
        String base="/api/compra/tiendas/"+slug+"/pedidos";
        mvc.perform(get(base).with(authentication(sesion(cliente)))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(pedido.toString()));
        mvc.perform(get(base+"/"+pedido).with(authentication(sesion(otro)))).andExpect(status().isNotFound());
        mvc.perform(get("/api/tiendas/"+tienda+"/pedidos/"+pedido).with(authentication(sesion(vendedor)))).andExpect(status().isOk());
        mvc.perform(get("/api/tiendas/"+tienda+"/pedidos").with(authentication(sesion(otro)))).andExpect(status().isForbidden());
        jdbc.update("INSERT INTO cuenta_pago_tienda(id_tienda,estado) VALUES (?,'PENDIENTE')",tienda);
        mvc.perform(post("/api/tiendas/"+tienda+"/cuenta-pago/desconexion").with(csrf()).with(authentication(sesion(vendedor)))).andExpect(status().isUnprocessableEntity());
    }
    @Test
    void checkoutInternoReservaInventarioYCongelaImportes() {
        UUID vendedor=UUID.randomUUID(),cliente=UUID.randomUUID(),tienda=UUID.randomUUID(),categoria=UUID.randomUUID(),producto=UUID.randomUUID(),presentacion=UUID.randomUUID(),carrito=UUID.randomUUID(),direccion=UUID.randomUUID();
        crearUsuario(vendedor); crearUsuario(cliente); String slug="t-"+tienda;
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')",tienda,vendedor,slug);
        jdbc.update("INSERT INTO cuenta_pago_tienda(id_tienda,estado,id_cuenta_externa,referencia_credencial,fecha_vinculacion) VALUES (?,'ACTIVA',?,'cifrado',now())",tienda,tienda.toString());
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Categoría','categoria')",categoria,tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base,estado) VALUES (?,?,?,'Producto',11.80,'PUBLICADO')",producto,categoria,tienda);
        jdbc.update("INSERT INTO presentacion_producto(id_presentacion,id_producto,id_tienda,sku) VALUES (?,?,?,'sku')",presentacion,producto,tienda);
        jdbc.update("INSERT INTO inventario(id_presentacion,stock_actual) VALUES (?,3)",presentacion);
        jdbc.update("INSERT INTO tienda_cliente(id_usuario,id_tienda) VALUES (?,?)",cliente,tienda);
        jdbc.update("INSERT INTO carrito(id_carrito,id_usuario,id_tienda) VALUES (?,?,?)",carrito,cliente,tienda);
        jdbc.update("INSERT INTO carrito_item(id_carrito,id_presentacion,cantidad) VALUES (?,?,2)",carrito,presentacion);
        jdbc.update("INSERT INTO ubigeo(codigo_ubigeo,departamento,provincia,distrito) VALUES ('999999','Prueba','Prueba','Prueba') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO direccion_cliente(id_direccion,id_usuario,codigo_ubigeo,direccion) VALUES (?,?,'999999','Calle')",direccion,cliente);
        assertThrows(com.ecommerce.backend.shared.exception.ReglaNegocioException.class,
                () -> checkout.reservarIdempotente(cliente,slug,direccion,"checkout_cambio_12345",new java.math.BigDecimal("20.00")));
        assertEquals(3,jdbc.queryForObject("SELECT stock_actual FROM inventario WHERE id_presentacion=?",Integer.class,presentacion));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM pedido WHERE id_carrito=?",Integer.class,carrito));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM checkout_idempotencia WHERE id_usuario=?",Integer.class,cliente));
        var resultado=checkout.reservarIdempotente(cliente,slug,direccion,"checkout_prueba_12345",new java.math.BigDecimal("23.60"));
        assertEquals(0,resultado.total().compareTo(new java.math.BigDecimal("23.60")));
        assertEquals(1,jdbc.queryForObject("SELECT stock_actual FROM inventario WHERE id_presentacion=?",Integer.class,presentacion));
        assertEquals("CERRADO",jdbc.queryForObject("SELECT estado FROM carrito WHERE id_carrito=?",String.class,carrito));
        assertEquals(0,jdbc.queryForObject("SELECT subtotal FROM pedido WHERE id_pedido=?",java.math.BigDecimal.class,resultado.idPedido()).compareTo(new java.math.BigDecimal("20.00")));
        UUID claveProveedor=jdbc.queryForObject("SELECT clave_proveedor FROM intento_order_mercadopago WHERE id_pedido=?",UUID.class,resultado.idPedido());
        var repetido=checkout.reservarIdempotente(cliente,slug,direccion,"checkout_prueba_12345",new java.math.BigDecimal("23.60"));
        assertEquals(resultado.idPedido(),repetido.idPedido());
        assertEquals(claveProveedor,jdbc.queryForObject("SELECT clave_proveedor FROM intento_order_mercadopago WHERE id_pedido=?",UUID.class,resultado.idPedido()));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM pedido WHERE id_carrito=?",Integer.class,carrito));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM intento_order_mercadopago WHERE id_pedido=?",Integer.class,resultado.idPedido()));
        assertEquals(1,jdbc.queryForObject("SELECT stock_actual FROM inventario WHERE id_presentacion=?",Integer.class,presentacion));
        assertThrows(com.ecommerce.backend.shared.exception.ReglaNegocioException.class,
                () -> checkout.reservarIdempotente(cliente,slug,direccion,"checkout_prueba_12345",new java.math.BigDecimal("24.00")));
        jdbc.update("UPDATE producto SET nombre='Modificado',precio_base=20 WHERE id_producto=?",producto);
        assertEquals("Producto",jdbc.queryForObject("SELECT nombre_producto FROM pedido_item WHERE id_pedido=?",String.class,resultado.idPedido()));
    }
}
