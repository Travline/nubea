package com.ecommerce.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ecommerce.backend.compra.CheckoutService;
import com.ecommerce.backend.shared.exception.MensajesUsuario;
import com.ecommerce.backend.shared.exception.TiendaNoDisponibleException;
import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.transaction.annotation.Transactional;

/** Bloque 2, puntos 3 y 4: tienda no disponible y disponibilidad de cada versión en la vitrina. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VitrinaDisponibilidadIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CheckoutService checkout;

    private UUID tienda, categoria, producto, idCliente;
    private String slug;
    private Authentication cliente;

    @BeforeEach
    void preparar() {
        UUID dueno = UUID.randomUUID();
        tienda = UUID.randomUUID(); categoria = UUID.randomUUID(); producto = UUID.randomUUID(); idCliente = UUID.randomUUID();
        slug = "t-" + tienda;
        for (UUID u : List.of(dueno, idCliente))
            jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Usuario', ?)", u, u + "@example.test");
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')", tienda, dueno, slug);
        jdbc.update("INSERT INTO configuracion_tienda(id_tienda,id_plantilla) VALUES (?,1)", tienda);
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Ropa','ropa')", categoria, tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base,estado) VALUES (?,?,?,'Polo',10,'PUBLICADO')", producto, categoria, tienda);
        cliente = UsernamePasswordAuthenticationToken.authenticated(idCliente, null, List.of());
    }

    private UUID version(String firma, int stock, int minimo) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO presentacion_producto(id_presentacion,id_producto,id_tienda,sku,firma_valores) VALUES (?,?,?,?,?)",
                id, producto, tienda, "SKU-" + firma, firma);
        jdbc.update("INSERT INTO inventario(id_presentacion,stock_actual,stock_minimo) VALUES (?,?,?)", id, stock, minimo);
        return id;
    }

    @Test
    void cadaVersionInformaSuDisponibilidadSinExponerElStockExacto() throws Exception {
        UUID agotada = version("a", 0, 0), pocas = version("b", 3, 0), muchas = version("c", 6, 0),
                bajoMinimo = version("d", 8, 10), sobreMinimo = version("e", 11, 10);
        String cuerpo = mvc.perform(get("/api/publico/tiendas/" + slug)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> versiones = JsonPath.read(cuerpo, "$.productos[0].presentaciones[*]");
        Map<String, Map<String, Object>> porId = new HashMap<>();
        versiones.forEach(v -> porId.put((String) v.get("id"), v));

        assertVersion(porId.get(agotada.toString()), "AGOTADO", null, false);
        assertVersion(porId.get(pocas.toString()), "ULTIMAS_UNIDADES", 3, true);
        assertVersion(porId.get(muchas.toString()), "DISPONIBLE", null, true);
        assertVersion(porId.get(bajoMinimo.toString()), "ULTIMAS_UNIDADES", 8, true);
        assertVersion(porId.get(sobreMinimo.toString()), "DISPONIBLE", null, true);
        assertFalse(cuerpo.contains("stock"), "La vitrina no debe exponer campos de stock");
    }

    private static void assertVersion(Map<String, Object> v, String disponibilidad, Integer restantes, boolean disponible) {
        assertNotNull(v);
        assertEquals(disponibilidad, v.get("disponibilidad"));
        assertEquals(disponible, v.get("disponible"));
        if (restantes == null) assertFalse(v.containsKey("unidadesRestantes"), "unidadesRestantes solo con ULTIMAS_UNIDADES");
        else assertEquals(restantes, v.get("unidadesRestantes"));
    }

    @Test
    void tiendaNoActivaNoMuestraCatalogoNiPermiteCarritoNiPago() throws Exception {
        UUID versionDisponible = version("x", 5, 0);
        for (String estado : List.of("SUSPENDIDA", "BORRADOR", "CERRADA")) {
            jdbc.update("UPDATE tienda SET estado=?, fecha_cierre=" + ("CERRADA".equals(estado) ? "now()" : "NULL") + " WHERE id_tienda=?", estado, tienda);

            mvc.perform(get("/api/publico/tiendas/" + slug)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("TIENDA_NO_DISPONIBLE"))
                    .andExpect(jsonPath("$.mensaje").value(MensajesUsuario.TIENDA_NO_DISPONIBLE))
                    .andExpect(jsonPath("$.productos").doesNotExist());
            mvc.perform(get("/api/compra/tiendas/" + slug + "/carrito").with(authentication(cliente)))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("TIENDA_NO_DISPONIBLE"));
            mvc.perform(post("/api/compra/tiendas/" + slug + "/carrito/items/" + versionDisponible).with(csrf()).with(authentication(cliente))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}"))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("TIENDA_NO_DISPONIBLE"));
            assertThrows(TiendaNoDisponibleException.class, () -> checkout.reservar(idCliente, slug, UUID.randomUUID()), estado);
        }
    }

    @Test
    void elCompradorVeSusPedidosAunqueLaTiendaNoEsteDisponible() throws Exception {
        UUID pedido = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda_cliente(id_usuario,id_tienda) VALUES (?,?)", idCliente, tienda);
        jdbc.update("""
            INSERT INTO pedido(id_pedido,id_usuario,id_tienda,id_impuesto,fecha_expiracion_reserva,subtotal,tasa_impuesto,monto_impuesto,total,
            nombre_receptor,direccion_entrega,ubigeo_entrega,departamento_entrega,provincia_entrega,distrito_entrega)
            VALUES (?,?,?,1,now()+interval '30 minutes',10,18,1.80,11.80,'Receptor','Calle','150101','Lima','Lima','Lima')
            """, pedido, idCliente, tienda);
        jdbc.update("UPDATE tienda SET estado='SUSPENDIDA' WHERE id_tienda=?", tienda);
        String base = "/api/compra/tiendas/" + slug + "/pedidos";
        mvc.perform(get(base).with(authentication(cliente))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(pedido.toString()));
        mvc.perform(get(base + "/" + pedido).with(authentication(cliente))).andExpect(status().isOk());
    }

    @Test
    void tiendaInexistenteSigueSiendoNoEncontrada() throws Exception {
        mvc.perform(get("/api/publico/tiendas/no-existe-" + UUID.randomUUID())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NO_ENCONTRADO"));
    }
}
