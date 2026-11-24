package com.ecommerce.backend;

import static com.ecommerce.backend.shared.exception.MensajesUsuario.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Bloque 1: SKU opcional (generado por tienda) y stock inicial al crear la presentación. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PresentacionSkuStockIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private Authentication dueno;
    private UUID tienda, producto, rojo, azul;
    private String slugTienda, ruta;

    @BeforeEach
    void preparar() {
        UUID usuario = UUID.randomUUID(), categoria = UUID.randomUUID(), color = UUID.randomUUID();
        tienda = UUID.randomUUID(); producto = UUID.randomUUID(); rojo = UUID.randomUUID(); azul = UUID.randomUUID();
        slugTienda = "mi-tienda-" + tienda.toString().substring(0, 8);
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Dueño', ?)", usuario, usuario + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='DUENO_TIENDA'", usuario);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Mi tienda',?,'ACTIVA')", tienda, usuario, slugTienda);
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Polos','polos')", categoria, tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base) VALUES (?,?,?,'Polo Básico',30)", producto, categoria, tienda);
        jdbc.update("INSERT INTO variante(id_variante,id_tienda,nombre) VALUES (?,?,'Color')", color, tienda);
        jdbc.update("INSERT INTO valor_variante(id_valor,id_variante,valor) VALUES (?,?,'Rojo'),(?,?,'Azul')", rojo, color, azul, color);
        dueno = UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of());
        ruta = "/api/tiendas/" + tienda + "/productos/" + producto + "/presentaciones";
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        return mvc.perform(peticion.with(csrf()).with(authentication(dueno)).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private Map<String, Object> inventario(String idPresentacion) {
        return jdbc.queryForMap("SELECT stock_actual, stock_minimo FROM inventario WHERE id_presentacion = ?", UUID.fromString(idPresentacion));
    }

    @Test
    void sinSkuSeGeneraUnoUnicoPorTiendaYConsecutivo() throws Exception {
        String prefijo = (slugTienda + "-polo-basico").toUpperCase();
        enviar(post(ruta), "{\"idsValores\":[\"" + rojo + "\"]}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sku").value(prefijo + "-001"));
        enviar(post(ruta), "{\"sku\":\"  \",\"idsValores\":[\"" + azul + "\"]}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sku").value(prefijo + "-002"));
    }

    @Test
    void elSkuGeneradoSaltaLosOcupadosYNoSuperaOchentaCaracteres() throws Exception {
        String prefijo = (slugTienda + "-polo-basico").toUpperCase();
        enviar(post(ruta), "{\"sku\":\"" + prefijo + "-001\",\"idsValores\":[\"" + rojo + "\"]}").andExpect(status().isCreated());
        enviar(post(ruta), "{\"idsValores\":[\"" + azul + "\"]}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sku").value(prefijo + "-002"));

        UUID largo = UUID.randomUUID();
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base) SELECT ?,id_categoria,?,?,10 FROM categoria WHERE id_tienda=?",
                largo, tienda, "Producto con un nombre extremadamente largo ".repeat(3).trim(), tienda);
        String respuesta = enviar(post("/api/tiendas/" + tienda + "/productos/" + largo + "/presentaciones"), "{}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String sku = JsonPath.read(respuesta, "$.sku");
        assertTrue(sku.length() <= 80, sku);
        assertTrue(sku.endsWith("-001"), sku);
        assertFalse(sku.contains("--"), sku);
    }

    @Test
    void stockInicialYMinimoSeGuardanEnElInventario() throws Exception {
        String conStock = JsonPath.read(enviar(post(ruta), "{\"idsValores\":[\"" + rojo + "\"],\"stockInicial\":5,\"stockMinimo\":2}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.idPresentacion");
        assertEquals(Map.of("stock_actual", 5, "stock_minimo", 2), inventario(conStock));

        String sinStock = JsonPath.read(enviar(post(ruta), "{\"idsValores\":[\"" + azul + "\"]}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.idPresentacion");
        assertEquals(Map.of("stock_actual", 0, "stock_minimo", 0), inventario(sinStock));
    }

    @Test
    void stockNegativoSeRechazaConCampo() throws Exception {
        enviar(post(ruta), "{\"stockInicial\":-1}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.stockInicial").value(UNIDADES_NEGATIVAS));
        enviar(post(ruta), "{\"stockMinimo\":-3}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.stockMinimo").value(UNIDADES_NEGATIVAS));
    }

    @Test
    void erroresDePresentacionLleganConCampoYSinJerga() throws Exception {
        String id = JsonPath.read(enviar(post(ruta), "{\"sku\":\"POLO-R\",\"idsValores\":[\"" + rojo + "\"]}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.idPresentacion");
        // SKU repetido
        enviar(post(ruta), "{\"sku\":\"POLO-R\",\"idsValores\":[\"" + azul + "\"]}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("DUPLICADO"))
                .andExpect(jsonPath("$.campos.sku").value(CODIGO_DUPLICADO));
        // Misma combinación de opciones
        enviar(post(ruta), "{\"idsValores\":[\"" + rojo + "\"]}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.campos.idsValores").value(COMBINACION_REPETIDA));
        // Dos valores de la misma opción
        enviar(post(ruta), "{\"idsValores\":[\"" + rojo + "\",\"" + azul + "\"]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.idsValores").value(UN_VALOR_POR_OPCION));
        // Al editar, el SKU sigue siendo obligatorio
        enviar(put(ruta + "/" + id), "{\"sku\":\"\",\"precioOverride\":null}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.sku").value(CODIGO_VACIO));
        enviar(put(ruta + "/" + id), "{\"sku\":\"POLO-ROJO\",\"precioOverride\":25}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.sku").value("POLO-ROJO"));
    }
}
