package com.ecommerce.backend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Bloque 4: las relaciones imagen ↔ versión responden DTOs, sin campos internos (storageKey, firmaValores, idTienda). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PresentacionImagenDtoIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void lasRelacionesImagenVersionNoExponenCamposInternos() throws Exception {
        UUID dueno = UUID.randomUUID(), tienda = UUID.randomUUID(), categoria = UUID.randomUUID(),
                producto = UUID.randomUUID(), presentacion = UUID.randomUUID(), imagen = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Dueño', ?)", dueno, dueno + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='DUENO_TIENDA'", dueno);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')", tienda, dueno, "t-" + tienda);
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Ropa','ropa')", categoria, tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base) VALUES (?,?,?,'Polo',10)", producto, categoria, tienda);
        jdbc.update("INSERT INTO presentacion_producto(id_presentacion,id_producto,id_tienda,sku,precio_override) VALUES (?,?,?,'POLO-R',12.5)", presentacion, producto, tienda);
        jdbc.update("INSERT INTO imagen_producto(id_imagen,id_producto,storage_key,orden,es_principal) VALUES (?,?,?,1,true)", imagen, producto, "productos/" + imagen + ".png");
        jdbc.update("INSERT INTO presentacion_imagen(id_presentacion,id_imagen) VALUES (?,?)", presentacion, imagen);
        var sesion = UsernamePasswordAuthenticationToken.authenticated(dueno, null, List.of());
        String base = "/api/tiendas/" + tienda;

        mvc.perform(get(base + "/presentaciones/" + presentacion + "/imagenes").with(authentication(sesion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idImagen").value(imagen.toString()))   // lo único que usa el cms actual
                .andExpect(jsonPath("$[0].url").value(org.hamcrest.Matchers.endsWith("productos/" + imagen + ".png")))
                .andExpect(jsonPath("$[0].orden").value(1))
                .andExpect(jsonPath("$[0].esPrincipal").value(true))
                .andExpect(jsonPath("$[0].storageKey").doesNotExist())
                .andExpect(jsonPath("$[0].idProducto").doesNotExist());

        mvc.perform(get(base + "/imagenes/" + imagen + "/presentaciones").with(authentication(sesion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPresentacion").value(presentacion.toString()))
                .andExpect(jsonPath("$[0].idProducto").value(producto.toString()))
                .andExpect(jsonPath("$[0].sku").value("POLO-R"))
                .andExpect(jsonPath("$[0].precioOverride").value(12.5))
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"))
                .andExpect(jsonPath("$[0].firmaValores").doesNotExist())
                .andExpect(jsonPath("$[0].idTienda").doesNotExist());
    }
}
