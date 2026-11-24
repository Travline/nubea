package com.ecommerce.backend;

import static com.ecommerce.backend.shared.exception.MensajesUsuario.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bloque 2, punto 2: ajustes +/−, "fijar stock" por conteo (unidades en mano − apartadas) y stockEsperado.
 * Mientras no exista el módulo de envíos, un pedido PAGADO se considera entregado: solo cuentan como
 * apartadas las unidades de pedidos pendientes de pago.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventarioAjustesIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private UUID tienda, presentacion, cliente;
    private Authentication dueno;
    private String ruta;

    @BeforeEach
    void preparar() {
        UUID idDueno = UUID.randomUUID(), categoria = UUID.randomUUID(), producto = UUID.randomUUID();
        tienda = UUID.randomUUID(); presentacion = UUID.randomUUID(); cliente = UUID.randomUUID();
        for (UUID u : List.of(idDueno, cliente))
            jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Usuario', ?)", u, u + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='DUENO_TIENDA'", idDueno);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')", tienda, idDueno, "t-" + tienda);
        jdbc.update("INSERT INTO tienda_cliente(id_usuario,id_tienda) VALUES (?,?)", cliente, tienda);
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Ropa','ropa')", categoria, tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base) VALUES (?,?,?,'Polo',5.90)", producto, categoria, tienda);
        jdbc.update("INSERT INTO presentacion_producto(id_presentacion,id_producto,id_tienda,sku) VALUES (?,?,?,'POLO')", presentacion, producto, tienda);
        jdbc.update("INSERT INTO inventario(id_presentacion,stock_actual,stock_minimo) VALUES (?,10,2)", presentacion);
        dueno = UsernamePasswordAuthenticationToken.authenticated(idDueno, null, List.of());
        ruta = "/api/tiendas/" + tienda + "/inventario/" + presentacion;
    }

    private ResultActions ajustar(String cuerpo) throws Exception {
        return mvc.perform(post(ruta + "/ajustes").with(csrf()).with(authentication(dueno)).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions fijar(String cuerpo) throws Exception {
        return mvc.perform(put(ruta).with(csrf()).with(authentication(dueno)).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private int stock() {
        return jdbc.queryForObject("SELECT stock_actual FROM inventario WHERE id_presentacion=?", Integer.class, presentacion);
    }

    /** Simula un checkout: pedido pendiente de pago con {@code cantidad} unidades y stock descontado (reserva ACTIVA). */
    private UUID pedidoPendiente(int cantidad) {
        UUID pedido = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO pedido(id_pedido,id_usuario,id_tienda,id_impuesto,fecha_expiracion_reserva,subtotal,tasa_impuesto,monto_impuesto,total,
            nombre_receptor,direccion_entrega,ubigeo_entrega,departamento_entrega,provincia_entrega,distrito_entrega)
            VALUES (?,?,?,1,now()+interval '30 minutes',?,18,?,?,'Receptor','Calle','150101','Lima','Lima','Lima')
            """, pedido, cliente, tienda, new java.math.BigDecimal("5.00").multiply(java.math.BigDecimal.valueOf(cantidad)),
                new java.math.BigDecimal("0.90").multiply(java.math.BigDecimal.valueOf(cantidad)),
                new java.math.BigDecimal("5.90").multiply(java.math.BigDecimal.valueOf(cantidad)));
        jdbc.update("""
            INSERT INTO pedido_item(id_pedido,id_presentacion,nombre_producto,cantidad,precio_unitario,subtotal,monto_impuesto,total_linea)
            VALUES (?,?,'Polo',?,5.90,?,?,?)
            """, pedido, presentacion, cantidad, new java.math.BigDecimal("5.00").multiply(java.math.BigDecimal.valueOf(cantidad)),
                new java.math.BigDecimal("0.90").multiply(java.math.BigDecimal.valueOf(cantidad)),
                new java.math.BigDecimal("5.90").multiply(java.math.BigDecimal.valueOf(cantidad)));
        jdbc.update("UPDATE inventario SET stock_actual = stock_actual - ? WHERE id_presentacion=?", cantidad, presentacion);
        return pedido;
    }

    @Test
    void ajustesRelativosNoDejanElStockEnNegativo() throws Exception {
        ajustar("{\"cantidad\":5}").andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(15)).andExpect(jsonPath("$.stockMinimo").value(2))
                .andExpect(jsonPath("$.stockReservado").value(0));
        ajustar("{\"cantidad\":-3}").andExpect(status().isOk()).andExpect(jsonPath("$.stockActual").value(12));
        ajustar("{\"cantidad\":-13}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICTO"))
                .andExpect(jsonPath("$.campos.cantidad").value(soloQuedanUnidades(12)));
        ajustar("{\"cantidad\":-12}").andExpect(status().isOk()).andExpect(jsonPath("$.stockActual").value(0));
        ajustar("{\"cantidad\":0}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.cantidad").value(AJUSTE_CERO));
        ajustar("{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.cantidad").exists());
        assertEquals(0, stock());
    }

    @Test
    void fijarConStockEsperadoDetectaCambiosSimultaneos() throws Exception {
        fijar("{\"stockActual\":20,\"stockMinimo\":3,\"stockEsperado\":9}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.campos.stockEsperado").value(STOCK_CAMBIO));
        assertEquals(10, stock());
        fijar("{\"stockActual\":20,\"stockMinimo\":3,\"stockEsperado\":10}").andExpect(status().isNoContent());
        assertEquals(20, stock());
        // Sin stockEsperado: compatible con el panel actual
        fijar("{\"stockActual\":7,\"stockMinimo\":1}").andExpect(status().isNoContent());
        assertEquals(7, stock());
    }

    @Test
    void fijarDescuentaLasApartadasYCancelarNoInflaElStock() throws Exception {
        UUID pedido = pedidoPendiente(2); // 10 en mano, 2 apartadas → 8 disponibles
        assertEquals(8, stock());
        mvc.perform(get("/api/tiendas/" + tienda + "/inventario").with(authentication(dueno))).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].stockActual").value(8))
                .andExpect(jsonPath("$.contenido[0].stockReservado").value(2));

        // El vendedor cuenta el estante: 10 unidades en mano.
        fijar("{\"stockActual\":10,\"stockMinimo\":2}").andExpect(status().isNoContent());
        assertEquals(8, stock());

        // El pedido vence: el trigger devuelve las 2 apartadas → 10, no 12.
        jdbc.update("UPDATE pedido SET estado='CANCELADO', estado_reserva='LIBERADA' WHERE id_pedido=?", pedido);
        assertEquals(10, stock());
    }

    @Test
    void elConteoNoPuedeSerMenorQueLasApartadasYLosPagadosNoCuentan() throws Exception {
        pedidoPendiente(3);
        fijar("{\"stockActual\":2,\"stockMinimo\":0}").andExpect(status().isConflict())
                .andExpect(jsonPath("$.campos.stockActual").value(unidadesApartadas(3)));

        // Un pedido PAGADO se considera entregado: sus unidades ya no están apartadas.
        UUID pagado = pedidoPendiente(1);
        jdbc.update("INSERT INTO pago(id_pedido,clave_idempotencia,estado,monto,fecha_confirmacion) VALUES (?,?,'APROBADO',5.90,now())",
                pagado, "test:" + pagado);
        jdbc.update("UPDATE pedido SET estado='PAGADO', estado_reserva='CONSUMIDA', fecha_pago=now() WHERE id_pedido=?", pagado);
        ajustar("{\"cantidad\":1}").andExpect(status().isOk()).andExpect(jsonPath("$.stockReservado").value(3));
        fijar("{\"stockActual\":3,\"stockMinimo\":0}").andExpect(status().isNoContent());
        assertEquals(0, stock());
    }

    @Test
    void conTiendaSuspendidaNoSeAjusta() throws Exception {
        jdbc.update("UPDATE tienda SET estado='SUSPENDIDA' WHERE id_tienda=?", tienda);
        ajustar("{\"cantidad\":1}").andExpect(status().isConflict());
        fijar("{\"stockActual\":1,\"stockMinimo\":0}").andExpect(status().isConflict());
        mvc.perform(post("/api/tiendas/" + tienda + "/inventario/" + UUID.randomUUID() + "/ajustes").with(csrf()).with(authentication(dueno))
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}")).andExpect(status().isConflict());
    }
}
