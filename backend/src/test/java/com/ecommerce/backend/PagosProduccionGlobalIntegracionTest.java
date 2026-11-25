package com.ecommerce.backend;

import static com.ecommerce.backend.CompraFixture.importe;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.backend.compra.MercadoPagoOrders;
import com.ecommerce.backend.cuentapago.oauth.CredencialesVendedorService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "mercadopago.pagos.enabled=true",
    "mercadopago.pagos.checkout-enabled=true",
    "mercadopago.pagos.produccion-global=true",
    "mercadopago.pagos.tiendas=",
    "mercadopago.pagos.delay-ms=3600000"
})
@AutoConfigureMockMvc
@Transactional
class PagosProduccionGlobalIntegracionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockitoBean MercadoPagoOrders mercado;
    @MockitoBean CredencialesVendedorService credenciales;

    @Test
    void cobraParaTiendasExistentesYNuevasSinConfigurarSusUuid() throws Exception {
        var tiendaExistente = new CompraFixture(jdbc);
        comprobarCobro(tiendaExistente);

        var tiendaNueva = new CompraFixture(jdbc);
        comprobarCobro(tiendaNueva);
    }

    @Test
    void noOfreceCheckoutParaTiendasInactivasOSinCuentaActiva() throws Exception {
        var tienda = new CompraFixture(jdbc);
        jdbc.update("UPDATE tienda SET estado='SUSPENDIDA' WHERE id_tienda=?", tienda.tienda);
        disponibilidad(tienda).andExpect(jsonPath("$.disponible").value(false));

        jdbc.update("UPDATE tienda SET estado='ACTIVA' WHERE id_tienda=?", tienda.tienda);
        jdbc.update("UPDATE cuenta_pago_tienda SET estado='ERROR' WHERE id_tienda=?", tienda.tienda);
        disponibilidad(tienda).andExpect(jsonPath("$.disponible").value(false));

        jdbc.update("DELETE FROM cuenta_pago_tienda WHERE id_tienda=?", tienda.tienda);
        disponibilidad(tienda).andExpect(jsonPath("$.disponible").value(false));
    }

    @Test
    void unaCuentaDePruebaNoCreaPedidoNiReservaStockEnProduccion() throws Exception {
        var tienda = new CompraFixture(jdbc);
        UUID presentacion = prepararCarrito(tienda);
        String token = simularCuenta(tienda, "PRUEBA");

        comprar(tienda).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").value("La cuenta del vendedor no corresponde al ambiente configurado"));

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM pedido WHERE id_tienda=?", Integer.class, tienda.tienda));
        assertEquals(3, tienda.stock(presentacion));
        verify(mercado).verificarCuenta(token, "cta-" + tienda.tienda);
        verifyNoMoreInteractions(mercado);
    }

    private void comprobarCobro(CompraFixture tienda) throws Exception {
        prepararCarrito(tienda);
        String token = simularCuenta(tienda, "PRODUCCION");
        String orden = "ORD-" + tienda.tienda;
        URI url = URI.create("https://www.mercadopago.com.pe/checkout/v1/redirect?o=" + orden);
        when(mercado.tiposExcluidos(token)).thenReturn(List.of("ticket"));
        when(mercado.crear(eq(token), any(), any(), any(), any(), any(), any()))
                .thenReturn(new MercadoPagoOrders.OrdenCreada(orden, url));
        when(mercado.consultar(token, orden)).thenAnswer(invocacion -> {
            UUID pedido = jdbc.queryForObject("SELECT id_pedido FROM pedido WHERE id_tienda=?", UUID.class, tienda.tienda);
            return new MercadoPagoOrders.OrdenConsultada(orden, pedido.toString(), "cta-" + tienda.tienda, "PEN",
                    importe("11.80"), importe("0.00"), "action_required", "waiting_payment");
        });

        disponibilidad(tienda).andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.ambiente").value("PRODUCCION"));
        comprar(tienda).andExpect(status().isOk())
                .andExpect(jsonPath("$.ambiente").value("PRODUCCION"))
                .andExpect(jsonPath("$.checkoutUrl").value(url.toString()));
    }

    private UUID prepararCarrito(CompraFixture tienda) {
        UUID presentacion = tienda.version(tienda.producto("PUBLICADO"), "ACTIVA", 3);
        tienda.alCarrito(presentacion, 1);
        return presentacion;
    }

    private String simularCuenta(CompraFixture tienda, String ambiente) {
        String cuenta = "cta-" + tienda.tienda;
        String token = "token-" + tienda.tienda;
        when(credenciales.obtener(tienda.tienda)).thenReturn(new CredencialesVendedorService.Token(cuenta, token));
        when(mercado.verificarCuenta(token, cuenta)).thenReturn(ambiente);
        return token;
    }

    private ResultActions disponibilidad(CompraFixture tienda) throws Exception {
        return mvc.perform(get("/api/compra/tiendas/{slug}/checkout", tienda.slug)
                .with(authentication(comprador(tienda))))
                .andExpect(status().isOk());
    }

    private ResultActions comprar(CompraFixture tienda) throws Exception {
        return mvc.perform(post("/api/compra/tiendas/{slug}/checkout", tienda.slug)
                .with(authentication(comprador(tienda))).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idDireccion":"%s","totalEsperado":11.80,"clave":"checkout_produccion_global"}
                        """.formatted(tienda.direccion)));
    }

    private UsernamePasswordAuthenticationToken comprador(CompraFixture tienda) {
        return UsernamePasswordAuthenticationToken.authenticated(tienda.comprador, null, List.of());
    }
}
