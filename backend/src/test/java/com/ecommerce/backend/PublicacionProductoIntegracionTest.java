package com.ecommerce.backend;

import static com.ecommerce.backend.shared.exception.MensajesUsuario.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Bloque 2, punto 6 (RN-M2-13): publicar exige una versión ACTIVA con unidades disponibles. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicacionProductoIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private UUID tienda, categoria, producto;
    private Authentication dueno;

    @BeforeEach
    void preparar() {
        UUID idDueno = UUID.randomUUID();
        tienda = UUID.randomUUID(); categoria = UUID.randomUUID(); producto = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Dueño', ?)", idDueno, idDueno + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='DUENO_TIENDA'", idDueno);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')", tienda, idDueno, "t-" + tienda);
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Ropa','ropa')", categoria, tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base) VALUES (?,?,?,'Polo',10)", producto, categoria, tienda);
        dueno = UsernamePasswordAuthenticationToken.authenticated(idDueno, null, List.of());
    }

    private void version(String firma, String estado, int stock) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO presentacion_producto(id_presentacion,id_producto,id_tienda,sku,estado,firma_valores) VALUES (?,?,?,?,?,?)",
                id, producto, tienda, "SKU-" + firma, estado, firma);
        jdbc.update("INSERT INTO inventario(id_presentacion,stock_actual) VALUES (?,?)", id, stock);
    }

    private ResultActions publicar() throws Exception {
        return mvc.perform(patch("/api/tiendas/" + tienda + "/productos/" + producto + "/estado")
                .param("estado", "PUBLICADO").with(csrf()).with(authentication(dueno)));
    }

    private void rechazado(String mensaje) throws Exception {
        publicar().andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("REGLA_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value(mensaje))
                .andExpect(jsonPath("$.campos.estado").value(mensaje));
    }

    @Test
    void conStockCeroSeRechazaYConStockSePublica() throws Exception {
        version("a", "ACTIVA", 0);
        rechazado(PUBLICAR_SIN_UNIDADES);
        jdbc.update("UPDATE inventario SET stock_actual=4 WHERE id_presentacion IN (SELECT id_presentacion FROM presentacion_producto WHERE id_producto=?)", producto);
        publicar().andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PUBLICADO"));
    }

    @Test
    void unaVersionInactivaConStockNoCuenta() throws Exception {
        version("a", "ACTIVA", 0);
        version("b", "INACTIVA", 9);
        rechazado(PUBLICAR_SIN_UNIDADES);
    }

    @Test
    void sinVersionActivaCategoriaInactivaOTiendaEnBorrador() throws Exception {
        rechazado(PUBLICAR_SIN_VERSION_ACTIVA);
        version("b", "INACTIVA", 9);
        rechazado(PUBLICAR_SIN_VERSION_ACTIVA);

        version("a", "ACTIVA", 3);
        jdbc.update("UPDATE categoria SET activa=false WHERE id_categoria=?", categoria);
        rechazado(PUBLICAR_CATEGORIA_INACTIVA);

        jdbc.update("UPDATE categoria SET activa=true WHERE id_categoria=?", categoria);
        jdbc.update("UPDATE tienda SET estado='BORRADOR' WHERE id_tienda=?", tienda);
        rechazado(PUBLICAR_TIENDA_INACTIVA);
    }

    // ---------- Bloque 3: checklist de requisitos sin intentar publicar ----------

    private ResultActions requisitos(Authentication quien) throws Exception {
        return mvc.perform(get("/api/tiendas/" + tienda + "/productos/" + producto + "/requisitos").with(authentication(quien)));
    }

    @Test
    void requisitosDevuelveElChecklistConCumplidoYMensaje() throws Exception {
        requisitos(dueno).andExpect(status().isOk())
                .andExpect(jsonPath("$.publicable").value(false))
                .andExpect(jsonPath("$.requisitos.length()").value(4))
                .andExpect(jsonPath("$.requisitos[0].codigo").value("TIENDA_ACTIVA"))
                .andExpect(jsonPath("$.requisitos[0].cumplido").value(true))
                .andExpect(jsonPath("$.requisitos[1].codigo").value("CATEGORIA_ACTIVA"))
                .andExpect(jsonPath("$.requisitos[1].cumplido").value(true))
                .andExpect(jsonPath("$.requisitos[2].codigo").value("VERSION_ACTIVA"))
                .andExpect(jsonPath("$.requisitos[2].cumplido").value(false))
                .andExpect(jsonPath("$.requisitos[2].mensaje").value(PUBLICAR_SIN_VERSION_ACTIVA))
                .andExpect(jsonPath("$.requisitos[3].codigo").value("VERSION_CON_UNIDADES"))
                .andExpect(jsonPath("$.requisitos[3].cumplido").value(false))
                .andExpect(jsonPath("$.requisitos[3].mensaje").value(PUBLICAR_SIN_UNIDADES));

        version("a", "ACTIVA", 0);
        version("b", "INACTIVA", 7);
        requisitos(dueno).andExpect(jsonPath("$.requisitos[2].cumplido").value(true))
                .andExpect(jsonPath("$.requisitos[3].cumplido").value(false)).andExpect(jsonPath("$.publicable").value(false));

        version("c", "ACTIVA", 2);
        requisitos(dueno).andExpect(jsonPath("$.publicable").value(true));
        // Consultar no publica
        org.junit.jupiter.api.Assertions.assertEquals("BORRADOR",
                jdbc.queryForObject("SELECT estado FROM producto WHERE id_producto=?", String.class, producto));
    }

    @Test
    void requisitosEsUnaLecturaParaElAdminPeroNoParaOtros() throws Exception {
        UUID admin = UUID.randomUUID(), otro = UUID.randomUUID();
        for (UUID u : List.of(admin, otro))
            jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'U', ?)", u, u + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='ADMIN_GENERAL'", admin);
        requisitos(UsernamePasswordAuthenticationToken.authenticated(admin, null, List.of())).andExpect(status().isOk());
        requisitos(UsernamePasswordAuthenticationToken.authenticated(otro, null, List.of())).andExpect(status().isForbidden());
    }
}
