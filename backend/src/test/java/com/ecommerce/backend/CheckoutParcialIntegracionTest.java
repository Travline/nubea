package com.ecommerce.backend;

import static com.ecommerce.backend.CompraFixture.importe;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ecommerce.backend.compra.CheckoutService;
import com.ecommerce.backend.shared.exception.MensajesUsuario;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Bloque 3, puntos 2 y 4: checkout parcial, carrito desglosado y número corto de pedido. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckoutParcialIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CheckoutService checkout;

    private CompraFixture f;
    private Authentication comprador, dueno;
    private UUID disponible, sinStock, inactiva, noPublicada;

    @BeforeEach
    void preparar() {
        f = new CompraFixture(jdbc);
        UUID publicado = f.producto("PUBLICADO");
        disponible = f.version(publicado, "ACTIVA", 10);
        sinStock = f.version(publicado, "ACTIVA", 1);       // se piden 3: se aparta la línea entera
        inactiva = f.version(publicado, "INACTIVA", 50);
        noPublicada = f.version(f.producto("BORRADOR"), "ACTIVA", 50);
        f.alCarrito(disponible, 2);
        f.alCarrito(sinStock, 3);
        f.alCarrito(inactiva, 1);
        f.alCarrito(noPublicada, 1);
        comprador = UsernamePasswordAuthenticationToken.authenticated(f.comprador, null, List.of());
        dueno = UsernamePasswordAuthenticationToken.authenticated(f.dueno, null, List.of());
    }

    private String clave() {
        return "checkout_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Test
    void elCarritoDesglosaSoloLoQueSeVaACobrar() throws Exception {
        mvc.perform(get("/api/compra/tiendas/" + f.slug + "/carrito").with(authentication(comprador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(4))
                .andExpect(jsonPath("$.total").value(82.60))        // todas las líneas (7 unidades), como antes
                .andExpect(jsonPath("$.subtotal").value(20.00))     // solo la línea disponible: 2 × 11.80 = 23.60
                .andExpect(jsonPath("$.igv").value(3.60))
                .andExpect(jsonPath("$.tasaIgv").value(18.00))
                .andExpect(jsonPath("$.totalAPagar").value(23.60));
    }

    @Test
    void unidadesDisponiblesSoloCuandoElStockNoAlcanza() throws Exception {
        String cuerpo = mvc.perform(get("/api/compra/tiendas/" + f.slug + "/carrito").with(authentication(comprador)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        java.util.List<java.util.Map<String, Object>> items = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.items[*]");
        java.util.Map<String, java.util.Map<String, Object>> porId = new java.util.HashMap<>();
        items.forEach(i -> porId.put((String) i.get("idPresentacion"), i));

        // Stock 1 para 3 pedidas: se informa cuántas quedan.
        assertEquals(1, porId.get(sinStock.toString()).get("unidadesDisponibles"));
        // Disponible, versión inactiva o producto no publicado: el stock no se expone.
        assertFalse(porId.get(disponible.toString()).containsKey("unidadesDisponibles"));
        assertFalse(porId.get(inactiva.toString()).containsKey("unidadesDisponibles"));
        assertFalse(porId.get(noPublicada.toString()).containsKey("unidadesDisponibles"));

        // Agotada (stock 0) y a la venta: 0 unidades.
        jdbc.update("UPDATE inventario SET stock_actual=0 WHERE id_presentacion=?", sinStock);
        mvc.perform(get("/api/compra/tiendas/" + f.slug + "/carrito").with(authentication(comprador)))
                .andExpect(jsonPath("$.items[?(@.idPresentacion=='" + sinStock + "')].unidadesDisponibles").value(org.hamcrest.Matchers.contains(0)));
    }

    @Test
    void compraSoloLosDisponiblesYApartaElRestoEnUnCarritoNuevo() {
        UUID carritoOriginal = f.carritoActivo();
        var resultado = checkout.reservarIdempotente(f.comprador, f.slug, f.direccion, clave(), importe("23.60"));

        assertEquals(0, resultado.total().compareTo(importe("23.60")));
        assertEquals(3, resultado.productosApartados());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pedido_item WHERE id_pedido=?", Integer.class, resultado.idPedido()));
        assertEquals(8, f.stock(disponible));
        assertEquals(1, f.stock(sinStock), "Una línea apartada no toca el stock");
        assertEquals("CERRADO", jdbc.queryForObject("SELECT estado FROM carrito WHERE id_carrito=?", String.class, carritoOriginal));

        UUID nuevo = f.carritoActivo();
        assertNotNull(nuevo);
        assertNotEquals(carritoOriginal, nuevo);
        assertNull(f.cantidadEnCarrito(disponible));
        assertEquals(3, f.cantidadEnCarrito(sinStock));
        assertEquals(1, f.cantidadEnCarrito(inactiva));
        assertEquals(1, f.cantidadEnCarrito(noPublicada));
    }

    @Test
    void sinNingunProductoDisponibleErrorClaroYElCarritoNoCambia() {
        jdbc.update("DELETE FROM carrito_item WHERE id_presentacion=?", disponible);
        UUID carrito = f.carritoActivo();
        var error = assertThrows(ReglaNegocioException.class,
                () -> checkout.reservarIdempotente(f.comprador, f.slug, f.direccion, clave(), importe("11.80")));
        assertEquals("items", error.getCampo());
        assertEquals(MensajesUsuario.NINGUN_PRODUCTO_DISPONIBLE, error.getMessage());
        assertEquals(carrito, f.carritoActivo());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM pedido WHERE id_tienda=?", Integer.class, f.tienda));
    }

    @Test
    void elTotalEsperadoEsElDeLasLineasDisponibles() {
        var error = assertThrows(ReglaNegocioException.class,
                () -> checkout.reservarIdempotente(f.comprador, f.slug, f.direccion, clave(), importe("82.60")));
        assertTrue(error.getMessage().contains("total"));
    }

    @Test
    void numeroCortoConsecutivoPorTiendaEnLasRespuestas() throws Exception {
        var primero = checkout.reservarIdempotente(f.comprador, f.slug, f.direccion, clave(), importe("23.60"));
        jdbc.update("UPDATE inventario SET stock_actual=10 WHERE id_presentacion=?", sinStock);
        var segundo = checkout.reservarIdempotente(f.comprador, f.slug, f.direccion, clave(), importe("35.40")); // 3 × 11.80
        assertEquals(1001, jdbc.queryForObject("SELECT numero FROM pedido WHERE id_pedido=?", Integer.class, primero.idPedido()));
        assertEquals(1002, jdbc.queryForObject("SELECT numero FROM pedido WHERE id_pedido=?", Integer.class, segundo.idPedido()));

        // Otra tienda empieza su propia numeración
        var otra = new CompraFixture(jdbc);
        otra.alCarrito(otra.version(otra.producto("PUBLICADO"), "ACTIVA", 5), 1);
        var deOtra = checkout.reservarIdempotente(otra.comprador, otra.slug, otra.direccion, clave(), importe("11.80"));
        assertEquals(1001, jdbc.queryForObject("SELECT numero FROM pedido WHERE id_pedido=?", Integer.class, deOtra.idPedido()));

        mvc.perform(get("/api/compra/tiendas/" + f.slug + "/pedidos").with(authentication(comprador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].numero").value(1002)).andExpect(jsonPath("$[1].numero").value(1001));
        mvc.perform(get("/api/compra/tiendas/" + f.slug + "/pedidos/" + primero.idPedido()).with(authentication(comprador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pedido.numero").value(1001));
        mvc.perform(get("/api/tiendas/" + f.tienda + "/pedidos").with(authentication(dueno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].numero").value(1002));
        mvc.perform(get("/api/tiendas/" + f.tienda + "/pedidos/" + segundo.idPedido()).with(authentication(dueno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pedido.numero").value(1002));
    }
}
