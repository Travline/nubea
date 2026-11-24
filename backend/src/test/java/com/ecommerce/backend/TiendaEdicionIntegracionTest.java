package com.ecommerce.backend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
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

/**
 * Escrituras sobre una tienda: requieren propietario + rol DUENO_TIENDA (RN-M1-10)
 * + tienda en estado modificable. Las lecturas del propietario siguen permitidas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TiendaEdicionIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    private static final String CATEGORIA = "{\"nombre\":\"Prueba\",\"slug\":\"prueba\"}";
    private static final String CONFIGURACION = "{\"idPlantilla\":1,\"tituloInicio\":\"Mi tienda\",\"mostrarCarrusel\":true}";
    private static final String INVENTARIO = "{\"stockActual\":3,\"stockMinimo\":1}";

    @Test
    void duenoConTiendaActivaPuedeEditarYConTiendaSuspendidaSoloLeer() throws Exception {
        UUID dueno = crearUsuario("Dueño", true);
        UUID tienda = crearTienda(dueno, "ACTIVA");
        Authentication sesion = sesion(dueno);
        String ruta = "/api/tiendas/" + tienda;

        // ACTIVA: escrituras permitidas
        mvc.perform(post(ruta + "/categorias").with(csrf()).with(authentication(sesion))
                .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA)).andExpect(status().isCreated());
        entityManager.flush();
        UUID categoria = jdbc.queryForObject("SELECT id_categoria FROM categoria WHERE id_tienda=?", UUID.class, tienda);
        mvc.perform(post(ruta + "/productos").with(csrf()).with(authentication(sesion)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Producto\",\"idCategoria\":\"" + categoria + "\",\"precioBase\":15.50}"))
                .andExpect(status().isCreated());
        entityManager.flush();
        UUID producto = jdbc.queryForObject("SELECT id_producto FROM producto WHERE id_tienda=?", UUID.class, tienda);
        mvc.perform(post(ruta + "/productos/" + producto + "/presentaciones").with(csrf()).with(authentication(sesion))
                .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"sku-edicion\"}")).andExpect(status().isCreated());
        entityManager.flush();
        UUID presentacion = jdbc.queryForObject("SELECT id_presentacion FROM presentacion_producto WHERE id_tienda=?", UUID.class, tienda);
        mvc.perform(put(ruta + "/inventario/" + presentacion).with(csrf()).with(authentication(sesion))
                .contentType(MediaType.APPLICATION_JSON).content(INVENTARIO)).andExpect(status().isNoContent());
        mvc.perform(patch(ruta + "/configuracion").with(csrf()).with(authentication(sesion))
                .contentType(MediaType.APPLICATION_JSON).content(CONFIGURACION)).andExpect(status().isOk());
        entityManager.flush();

        // SUSPENDIDA: escrituras rechazadas con 409, lecturas permitidas
        jdbc.update("UPDATE tienda SET estado='SUSPENDIDA' WHERE id_tienda=?", tienda);
        entityManager.clear();
        mvc.perform(post(ruta + "/categorias").with(csrf()).with(authentication(sesion))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Otra\",\"slug\":\"otra\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICTO"));
        mvc.perform(patch(ruta + "/configuracion").with(csrf()).with(authentication(sesion))
                .contentType(MediaType.APPLICATION_JSON).content(CONFIGURACION)).andExpect(status().isConflict());
        mvc.perform(get(ruta + "/categorias").with(authentication(sesion))).andExpect(status().isOk());
        mvc.perform(get(ruta + "/productos").with(authentication(sesion))).andExpect(status().isOk());
        mvc.perform(get(ruta + "/inventario").with(authentication(sesion))).andExpect(status().isOk());
        mvc.perform(get(ruta + "/configuracion").with(authentication(sesion))).andExpect(status().isOk());
        mvc.perform(get(ruta + "/banners").with(authentication(sesion))).andExpect(status().isOk());
        mvc.perform(get(ruta + "/pedidos").with(authentication(sesion))).andExpect(status().isOk());
        // Desconectar Mercado Pago sigue permitido aun con la tienda suspendida
        mvc.perform(post(ruta + "/cuenta-pago/desconexion").with(csrf()).with(authentication(sesion)))
                .andExpect(status().isNoContent());
        // Al final: el PUT de inventario es @Transactional y un rechazo marca la transacción del test
        mvc.perform(put(ruta + "/inventario/" + presentacion).with(csrf()).with(authentication(sesion))
                .contentType(MediaType.APPLICATION_JSON).content(INVENTARIO)).andExpect(status().isConflict());
    }

    @Test
    void cerradaTambienRechazaEscrituras() throws Exception {
        UUID dueno = crearUsuario("Dueño", true);
        UUID tienda = crearTienda(dueno, "CERRADA");
        mvc.perform(post("/api/tiendas/" + tienda + "/categorias").with(csrf()).with(authentication(sesion(dueno)))
                .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA)).andExpect(status().isConflict());
    }

    @Test
    void propietarioSinRolDuenoNoPuedeEscribirPeroSiLeer() throws Exception {
        UUID dueno = crearUsuario("Sin rol", false);
        UUID tienda = crearTienda(dueno, "ACTIVA");
        Authentication sesion = sesion(dueno);
        String ruta = "/api/tiendas/" + tienda;
        mvc.perform(post(ruta + "/categorias").with(csrf()).with(authentication(sesion))
                .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA)).andExpect(status().isForbidden());
        mvc.perform(patch(ruta + "/configuracion").with(csrf()).with(authentication(sesion))
                .contentType(MediaType.APPLICATION_JSON).content(CONFIGURACION)).andExpect(status().isForbidden());
        mvc.perform(get(ruta + "/categorias").with(authentication(sesion))).andExpect(status().isOk());
        mvc.perform(get(ruta + "/configuracion").with(authentication(sesion))).andExpect(status().isOk());
    }

    @Test
    void ajenoConRolDuenoNoPuedeEscribirEnTiendaQueNoEsSuya() throws Exception {
        UUID dueno = crearUsuario("Dueño", true), ajeno = crearUsuario("Ajeno", true);
        UUID tienda = crearTienda(dueno, "ACTIVA");
        mvc.perform(post("/api/tiendas/" + tienda + "/categorias").with(csrf()).with(authentication(sesion(ajeno)))
                .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA)).andExpect(status().isForbidden());
    }

    @Test
    void retirarRolDuenoBloqueaEscriturasDesdeLaSiguientePeticion() throws Exception {
        UUID dueno = crearUsuario("Dueño", true), admin = crearUsuario("Admin", false);
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='ADMIN_GENERAL'", admin);
        UUID tienda = crearTienda(dueno, "ACTIVA");
        Authentication sesion = sesion(dueno);
        String ruta = "/api/tiendas/" + tienda;
        mvc.perform(post(ruta + "/categorias").with(csrf()).with(authentication(sesion))
                .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA)).andExpect(status().isCreated());
        mvc.perform(delete("/api/usuarios/" + dueno + "/roles/DUENO_TIENDA").with(csrf()).with(authentication(sesion(admin))))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();
        mvc.perform(post(ruta + "/categorias").with(csrf()).with(authentication(sesion))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Otra\",\"slug\":\"otra\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get(ruta + "/categorias").with(authentication(sesion))).andExpect(status().isOk());
    }

    private UUID crearUsuario(String nombre, boolean conRolDueno) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?,?,?)", id, nombre, id + "@example.test");
        if (conRolDueno) {
            jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='DUENO_TIENDA'", id);
        }
        return id;
    }

    private UUID crearTienda(UUID propietario, String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado,fecha_cierre) VALUES (?,?,'Tienda',?,?,"
                + ("CERRADA".equals(estado) ? "now()" : "NULL") + ")", id, propietario, "t-" + id, estado);
        jdbc.update("INSERT INTO configuracion_tienda(id_tienda,id_plantilla) VALUES (?,1)", id);
        jdbc.update("INSERT INTO cuenta_pago_tienda(id_tienda) VALUES (?)", id);
        return id;
    }

    private static Authentication sesion(UUID usuario) {
        return UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of());
    }
}
