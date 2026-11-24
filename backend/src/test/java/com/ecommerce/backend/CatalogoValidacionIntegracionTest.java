package com.ecommerce.backend;

import static com.ecommerce.backend.shared.exception.MensajesUsuario.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
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

/** Bloque 1: slugs automáticos de categoría y colección, y errores con campo para el catálogo. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogoValidacionIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private Authentication dueno;
    private String base;

    @BeforeEach
    void preparar() {
        UUID usuario = UUID.randomUUID(), tienda = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Dueño', ?)", usuario, usuario + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='DUENO_TIENDA'", usuario);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')", tienda, usuario, "t-" + tienda);
        dueno = UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of());
        base = "/api/tiendas/" + tienda;
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        return mvc.perform(peticion.with(csrf()).with(authentication(dueno)).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private String crear(String recurso, String cuerpo, String campoId) throws Exception {
        String respuesta = enviar(post(base + "/" + recurso), cuerpo).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$." + campoId);
    }

    // ---------- 1. Slugs automáticos ----------

    @Test
    void categoriaSinSlugLoGeneraYAseguraUnicidadEnLaTienda() throws Exception {
        enviar(post(base + "/categorias"), "{\"nombre\":\"Ropa de Niño\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.slug").value("ropa-de-nino"));
        enviar(post(base + "/categorias"), "{\"nombre\":\"Ropa de niño\",\"slug\":\"\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.slug").value("ropa-de-nino-2"));
        enviar(post(base + "/categorias"), "{\"nombre\":\"ROPA DE NIÑO\",\"slug\":null}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.slug").value("ropa-de-nino-3"));
        enviar(post(base + "/categorias"), "{\"nombre\":\"¡¡!!\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.slug").value("categoria"));
    }

    @Test
    void categoriaConSlugInvalidoODuplicadoDevuelveErrorConCampo() throws Exception {
        enviar(post(base + "/categorias"), "{\"nombre\":\"Ropa\",\"slug\":\"ropa\"}").andExpect(status().isCreated());
        enviar(post(base + "/categorias"), "{\"nombre\":\"Otra\",\"slug\":\"Mi Ropa\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.mensaje").value(DIRECCION_FORMATO))
                .andExpect(jsonPath("$.campos.slug").value(DIRECCION_FORMATO));
        enviar(post(base + "/categorias"), "{\"nombre\":\"Otra\",\"slug\":\"ropa\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICADO"))
                .andExpect(jsonPath("$.campos.slug").value(CATEGORIA_DIRECCION_DUPLICADA));
        enviar(post(base + "/categorias"), "{\"nombre\":\"Larga\",\"slug\":\"" + "a".repeat(121) + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.slug").exists());
    }

    @Test
    void editarElNombreDeUnaCategoriaNoCambiaSuSlug() throws Exception {
        String id = crear("categorias", "{\"nombre\":\"Zapatos\"}", "idCategoria");
        enviar(put(base + "/categorias/" + id), "{\"nombre\":\"Calzado de temporada\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Calzado de temporada"))
                .andExpect(jsonPath("$.slug").value("zapatos"));
        // El cms actual envía el slug al editar: sigue funcionando y se valida.
        enviar(put(base + "/categorias/" + id), "{\"nombre\":\"Calzado\",\"slug\":\"calzado\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("calzado"));
        enviar(put(base + "/categorias/" + id), "{\"nombre\":\"Calzado\",\"slug\":\"Calzado Nuevo\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.slug").value(DIRECCION_FORMATO));
    }

    @Test
    void coleccionSinSlugLoGeneraYAlEditarLoConserva() throws Exception {
        enviar(post(base + "/colecciones"), "{\"nombre\":\"Verano 2026\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.slug").value("verano-2026"));
        String id = crear("colecciones", "{\"nombre\":\"Verano 2026\"}", "idColeccion");
        enviar(get(base + "/colecciones/" + id), "").andExpect(jsonPath("$.slug").value("verano-2026-2"));
        enviar(put(base + "/colecciones/" + id), "{\"nombre\":\"Rebajas de verano\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("verano-2026-2"));
        enviar(post(base + "/colecciones"), "{\"nombre\":\"Otra\",\"slug\":\"verano-2026\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.campos.slug").value(COLECCION_DIRECCION_DUPLICADA));
        enviar(post(base + "/colecciones"), "{\"nombre\":\"Otra\",\"slug\":\"verano_2026\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.slug").value(DIRECCION_FORMATO));
    }

    // ---------- 2. Errores claros en opciones y valores ----------

    @Test
    void nombreDeOpcionRespetaElLimiteDeLaBdYNoSeRepite() throws Exception {
        enviar(post(base + "/variantes"), "{\"nombre\":\"" + "a".repeat(81) + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.nombre").exists());
        enviar(post(base + "/variantes"), "{\"nombre\":\"" + "a".repeat(80) + "\"}").andExpect(status().isCreated());
        String color = crear("variantes", "{\"nombre\":\"Color\"}", "idVariante");
        enviar(post(base + "/variantes"), "{\"nombre\":\"color\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICADO"))
                .andExpect(jsonPath("$.campos.nombre").value(OPCION_DUPLICADA));
        enviar(post(base + "/variantes/" + color + "/valores"), "{\"valor\":\"Rojo\"}").andExpect(status().isCreated());
        enviar(post(base + "/variantes/" + color + "/valores"), "{\"valor\":\"ROJO\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.campos.valor").value(VALOR_DUPLICADO));
    }
}
