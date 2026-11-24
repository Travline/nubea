package com.ecommerce.backend;

import static com.ecommerce.backend.shared.exception.MensajesUsuario.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Bloque 2, punto 1: el ADMIN_GENERAL ve, suspende y reactiva; no edita contenido ni ve pedidos o pagos. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminTiendasIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    private UUID dueno, tienda, producto, presentacion;
    private Authentication admin, sesionDueno, otro;
    private String base;

    @BeforeEach
    void preparar() {
        dueno = UUID.randomUUID(); tienda = UUID.randomUUID(); producto = UUID.randomUUID(); presentacion = UUID.randomUUID();
        UUID idAdmin = UUID.randomUUID(), idOtro = UUID.randomUUID(), categoria = UUID.randomUUID();
        for (UUID u : List.of(dueno, idAdmin, idOtro))
            jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Usuario', ?)", u, u + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='DUENO_TIENDA'", dueno);
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='ADMIN_GENERAL'", idAdmin);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')", tienda, dueno, "t-" + tienda);
        jdbc.update("INSERT INTO configuracion_tienda(id_tienda,id_plantilla) VALUES (?,1)", tienda);
        jdbc.update("INSERT INTO cuenta_pago_tienda(id_tienda) VALUES (?)", tienda);
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Ropa','ropa')", categoria, tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base) VALUES (?,?,?,'Polo',10)", producto, categoria, tienda);
        jdbc.update("INSERT INTO presentacion_producto(id_presentacion,id_producto,id_tienda,sku) VALUES (?,?,?,'POLO')", presentacion, producto, tienda);
        jdbc.update("INSERT INTO inventario(id_presentacion,stock_actual) VALUES (?,5)", presentacion);
        admin = UsernamePasswordAuthenticationToken.authenticated(idAdmin, null, List.of());
        sesionDueno = UsernamePasswordAuthenticationToken.authenticated(dueno, null, List.of());
        otro = UsernamePasswordAuthenticationToken.authenticated(idOtro, null, List.of());
        base = "/api/tiendas/" + tienda;
    }

    private org.springframework.test.web.servlet.ResultActions estado(Authentication quien, String estado) throws Exception {
        return mvc.perform(patch("/api/admin/tiendas/" + tienda + "/estado").with(csrf()).with(authentication(quien))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"" + estado + "\"}"));
    }

    @Test
    void adminNoEditaContenidoDeTiendasAjenas() throws Exception {
        mvc.perform(post(base + "/categorias").with(csrf()).with(authentication(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Otra\"}")).andExpect(status().isForbidden());
        mvc.perform(put(base + "/productos/" + producto).with(csrf()).with(authentication(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"X\",\"precioBase\":1,\"idCategoria\":\"" + UUID.randomUUID() + "\"}")).andExpect(status().isForbidden());
        mvc.perform(patch(base + "/productos/" + producto + "/estado").param("estado", "INACTIVO").with(csrf()).with(authentication(admin)))
                .andExpect(status().isForbidden());
        mvc.perform(put(base + "/inventario/" + presentacion).with(csrf()).with(authentication(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"stockActual\":1,\"stockMinimo\":0}")).andExpect(status().isForbidden());
        mvc.perform(post(base + "/inventario/" + presentacion + "/ajustes").with(csrf()).with(authentication(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}")).andExpect(status().isForbidden());
        mvc.perform(multipart(base + "/productos/" + producto + "/imagenes")
                .file(new MockMultipartFile("archivo", "a.png", "image/png", new byte[]{1})).param("orden", "1")
                .with(csrf()).with(authentication(admin))).andExpect(status().isForbidden());
        mvc.perform(patch(base + "/configuracion").with(csrf()).with(authentication(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"tituloInicio\":\"X\"}")).andExpect(status().isForbidden());
    }

    @Test
    void adminLeeCatalogoAparienciaInventarioYCuentaPeroNoPedidos() throws Exception {
        for (String ruta : List.of("/categorias", "/productos", "/productos/" + producto + "/presentaciones", "/variantes",
                "/colecciones", "/inventario", "/configuracion", "/banners", "/cuenta-pago")) {
            mvc.perform(get(base + ruta).with(authentication(admin))).andExpect(status().isOk());
            mvc.perform(get(base + ruta).with(authentication(otro))).andExpect(status().isForbidden());
        }
        // Pedidos y su detalle contienen datos personales del comprador: solo el dueño.
        mvc.perform(get(base + "/pedidos").with(authentication(admin))).andExpect(status().isForbidden());
        mvc.perform(get(base + "/pedidos/" + UUID.randomUUID()).with(authentication(admin))).andExpect(status().isForbidden());
        mvc.perform(get(base + "/pedidos").with(authentication(sesionDueno))).andExpect(status().isOk());
    }

    @Test
    void adminSuspendeYReactivaDejandoLaTiendaEnBorrador() throws Exception {
        estado(admin, "SUSPENDIDA").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("SUSPENDIDA"));
        estado(admin, "SUSPENDIDA").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("SUSPENDIDA")); // idempotente
        em.flush();
        mvc.perform(post(base + "/categorias").with(csrf()).with(authentication(sesionDueno))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Otra\"}")).andExpect(status().isConflict());
        mvc.perform(get("/api/publico/tiendas/t-" + tienda)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TIENDA_NO_DISPONIBLE"));

        estado(admin, "ACTIVA").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("BORRADOR"));
        em.flush();
        mvc.perform(post(base + "/categorias").with(csrf()).with(authentication(sesionDueno))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Otra\"}")).andExpect(status().isCreated());
    }

    @Test
    void reactivarExigeTiendaSuspendidaYDuenoConRol() throws Exception {
        estado(admin, "ACTIVA").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICTO"))
                .andExpect(jsonPath("$.campos.estado").value(SOLO_SE_REACTIVA_SUSPENDIDA));

        // Retirar el rol de dueño suspende la tienda (RN-M1-10); reactivar sin devolverlo se rechaza.
        mvc.perform(delete("/api/usuarios/" + dueno + "/roles/DUENO_TIENDA").with(csrf()).with(authentication(admin)))
                .andExpect(status().isNoContent());
        em.flush();
        estado(admin, "ACTIVA").andExpect(status().isConflict())
                .andExpect(jsonPath("$.campos.estado").value(DUENO_SIN_PERMISO_VENDEDOR));

        mvc.perform(put("/api/usuarios/" + dueno + "/roles/DUENO_TIENDA").with(csrf()).with(authentication(admin)))
                .andExpect(status().isNoContent());
        em.flush();
        estado(admin, "ACTIVA").andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    void tiendaCerradaEstadoInvalidoYUsuarioSinPermiso() throws Exception {
        estado(admin, "CERRADA").andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.estado").value(ESTADO_ADMIN_INVALIDO));
        estado(sesionDueno, "SUSPENDIDA").andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/tiendas/" + UUID.randomUUID() + "/estado").with(csrf()).with(authentication(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"SUSPENDIDA\"}")).andExpect(status().isNotFound());

        jdbc.update("UPDATE tienda SET estado='CERRADA', fecha_cierre=now() WHERE id_tienda=?", tienda);
        estado(admin, "SUSPENDIDA").andExpect(status().isConflict()).andExpect(jsonPath("$.campos.estado").value(TIENDA_CERRADA_SIN_CAMBIOS));
        estado(admin, "ACTIVA").andExpect(status().isConflict()).andExpect(jsonPath("$.campos.estado").value(TIENDA_CERRADA_SIN_CAMBIOS));
    }

    @Test
    void adminVeElDuenoLegibleYBuscaPorSuCorreo() throws Exception {
        jdbc.update("UPDATE usuario_plataforma SET nombres='Ana', apellido_paterno='Pérez' WHERE id_usuario=?", dueno);
        String correo = dueno + "@example.test";
        mvc.perform(get("/api/admin/tiendas").param("texto", correo).with(authentication(admin))).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].idTienda").value(tienda.toString()))
                .andExpect(jsonPath("$.contenido[0].nombrePropietario").value("Ana Pérez"))
                .andExpect(jsonPath("$.contenido[0].correoPropietario").value(correo));
        mvc.perform(get("/api/admin/tiendas/" + tienda).with(authentication(admin))).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Tienda")).andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.nombrePropietario").value("Ana Pérez"));
        mvc.perform(get("/api/admin/tiendas/" + UUID.randomUUID()).with(authentication(admin))).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/tiendas/" + tienda).with(authentication(sesionDueno))).andExpect(status().isForbidden());
        estado(admin, "SUSPENDIDA").andExpect(status().isOk()).andExpect(jsonPath("$.correoPropietario").value(correo));
    }

    @Test
    void tiendaNoModificableResponde409ConMensajeParaElUsuario() throws Exception {
        estado(admin, "SUSPENDIDA").andExpect(status().isOk());
        em.flush();
        mvc.perform(post(base + "/categorias").with(csrf()).with(authentication(sesionDueno))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Otra\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICTO"))
                .andExpect(jsonPath("$.mensaje").value(TIENDA_SUSPENDIDA_SOLO_LECTURA));
        mvc.perform(post(base + "/inventario/" + presentacion + "/ajustes").with(csrf()).with(authentication(sesionDueno))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(TIENDA_SUSPENDIDA_SOLO_LECTURA));

        jdbc.update("UPDATE tienda SET estado='CERRADA', fecha_cierre=now() WHERE id_tienda=?", tienda);
        mvc.perform(patch(base + "/configuracion").with(csrf()).with(authentication(sesionDueno))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tituloInicio\":\"X\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(TIENDA_CERRADA_SOLO_LECTURA));
    }
}
