package com.ecommerce.backend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ecommerce.backend.identidad.repository.UsuarioPlataformaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdentidadIntegracionTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioPlataformaRepository usuarios;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired com.ecommerce.backend.identidad.service.InicializacionAdministrador bootstrap;
    @Autowired com.ecommerce.backend.identidad.service.UsuarioService usuarioService;
    @Autowired com.ecommerce.backend.tienda.service.TiendaService tiendaService;
    @Autowired org.springframework.transaction.support.TransactionTemplate transacciones;

    @Test
    void administradorGestionaPlantillasYRolesSinCambiarSusPropiosRoles() throws Exception {
        UUID admin = UUID.randomUUID(), usuario = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Admin', ?), (?, 'Usuario', ?)", admin, admin + "@example.test", usuario, usuario + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='ADMIN_GENERAL'", admin);
        var administrador = UsernamePasswordAuthenticationToken.authenticated(admin, null, List.of());
        var otro = UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of());
        String codigo = "plantilla-" + UUID.randomUUID();
        String cuerpo = "{\"codigo\":\"" + codigo + "\",\"nombre\":\"Plantilla prueba\",\"activa\":false}";
        mvc.perform(post("/api/plantillas").with(csrf()).with(authentication(otro)).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/plantillas").with(csrf()).with(authentication(administrador)).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.activa").value(false));
        Short id = jdbc.queryForObject("SELECT id_plantilla FROM plantilla WHERE codigo = ?", Short.class, codigo);
        mvc.perform(patch("/api/plantillas/" + id).with(csrf()).with(authentication(administrador)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Actualizada\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Actualizada"));
        mvc.perform(patch("/api/plantillas/" + id + "/estado").with(csrf()).with(authentication(administrador)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"activa\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(true));
        mvc.perform(put("/api/usuarios/" + usuario + "/roles/DUENO_TIENDA").with(csrf()).with(authentication(administrador)))
                .andExpect(status().isNoContent());
        entityManager.flush();
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM usuario_rol WHERE id_usuario = ?", Integer.class, admin));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM usuario_rol ur JOIN rol r ON r.id_rol = ur.id_rol WHERE ur.id_usuario = ? AND r.nombre = 'DUENO_TIENDA'", Integer.class, usuario));
    }

    @Test
    void administracionListaUsuariosYTiendasSoloParaAdministrador() throws Exception {
        UUID admin = UUID.randomUUID(), vendedor = UUID.randomUUID(), tienda = UUID.randomUUID();
        String clave = UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Admin', ?), (?, ?, ?)", admin, admin + "@example.test", vendedor, "Vendedor " + clave, vendedor + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='ADMIN_GENERAL'", admin);
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='DUENO_TIENDA'", vendedor);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug) VALUES (?,?,?,?)", tienda, vendedor, "Tienda " + clave, "tienda-" + clave);
        var administrador = UsernamePasswordAuthenticationToken.authenticated(admin, null, List.of());
        var propietario = UsernamePasswordAuthenticationToken.authenticated(vendedor, null, List.of());
        for (String ruta : List.of("/api/admin/usuarios", "/api/admin/tiendas")) {
            mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            mvc.perform(get(ruta).with(authentication(propietario))).andExpect(status().isForbidden());
            mvc.perform(get(ruta).with(authentication(administrador)).param("size", "101")).andExpect(status().isBadRequest());
            mvc.perform(get(ruta).with(authentication(administrador)).param("page", "-1")).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/admin/usuarios").with(authentication(administrador)).param("texto", clave).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].idUsuario").value(vendedor.toString()))
                .andExpect(jsonPath("$.contenido[0].roles[0]").value("DUENO_TIENDA"));
        mvc.perform(get("/api/admin/tiendas").with(authentication(administrador)).param("texto", clave).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].idTienda").value(tienda.toString()))
                .andExpect(jsonPath("$.contenido[0].idPropietario").value(vendedor.toString()));
        mvc.perform(get("/api/admin/usuarios").with(authentication(administrador)).param("texto", clave).param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido.length()").value(0));
    }

    @Test
    void healthEsPublicoYCompruebaLaBase() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void aparienciaYBannersExigenPropiedadYPlantillasExigenRol() throws Exception {
        UUID dueno = UUID.randomUUID(), ajeno = UUID.randomUUID(), tienda = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Dueño', ?), (?, 'Ajeno', ?)", dueno, dueno + "@example.test", ajeno, ajeno + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?, id_rol FROM rol WHERE nombre='DUENO_TIENDA'", dueno);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')", tienda, dueno, "t-" + tienda);
        jdbc.update("INSERT INTO configuracion_tienda(id_tienda,id_plantilla) VALUES (?,1)", tienda);
        var propietario = UsernamePasswordAuthenticationToken.authenticated(dueno, null, List.of());
        var otro = UsernamePasswordAuthenticationToken.authenticated(ajeno, null, List.of());
        String ruta = "/api/tiendas/" + tienda;
        mvc.perform(get(ruta + "/configuracion").with(authentication(propietario))).andExpect(status().isOk());
        mvc.perform(get(ruta + "/configuracion").with(authentication(otro))).andExpect(status().isForbidden());
        mvc.perform(get(ruta + "/banners").with(authentication(propietario))).andExpect(status().isOk());
        mvc.perform(get(ruta + "/banners").with(authentication(otro))).andExpect(status().isForbidden());
        mvc.perform(get("/api/plantillas").with(authentication(propietario))).andExpect(status().isOk());
        mvc.perform(get("/api/plantillas").with(authentication(otro))).andExpect(status().isForbidden());
    }

    @Test
    void inicializaSoloElPrimerAdministrador() {
        UUID primero = UUID.randomUUID(), segundo = UUID.randomUUID();
        String email = primero + "@example.test";
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Primero', ?), (?, 'Segundo', ?)", primero, email, segundo, segundo + "@example.test");
        try {
            org.springframework.test.util.ReflectionTestUtils.setField(bootstrap, "email", email);
            bootstrap.run(null);
            org.springframework.test.util.ReflectionTestUtils.setField(bootstrap, "email", segundo + "@example.test");
            bootstrap.run(null);
            org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM usuario_rol ur JOIN rol r ON r.id_rol=ur.id_rol WHERE r.nombre='ADMIN_GENERAL' AND ur.id_usuario IN (?,?)", Integer.class, primero, segundo));
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(bootstrap, "email", "");
        }
    }

    @Test
    void soloAdministradorRetiraRolYSuspendeTiendas() throws Exception {
        UUID admin = UUID.randomUUID(), dueno = UUID.randomUUID(), tienda = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Admin', ?), (?, 'Dueño', ?)", admin, admin + "@example.test", dueno, dueno + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?, id_rol FROM rol WHERE nombre = 'ADMIN_GENERAL'", admin);
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?, id_rol FROM rol WHERE nombre = 'DUENO_TIENDA'", dueno);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')", tienda, dueno, "t-" + tienda);
        String ruta = "/api/usuarios/" + dueno + "/roles/DUENO_TIENDA";
        var noAdmin = UsernamePasswordAuthenticationToken.authenticated(dueno, null, List.of());
        mvc.perform(delete(ruta).with(csrf()).with(authentication(noAdmin))).andExpect(status().isForbidden());
        var administrador = UsernamePasswordAuthenticationToken.authenticated(admin, null, List.of());
        mvc.perform(delete(ruta).with(csrf()).with(authentication(administrador))).andExpect(status().isNoContent());
        entityManager.flush();
        org.junit.jupiter.api.Assertions.assertEquals("SUSPENDIDA", jdbc.queryForObject("SELECT estado FROM tienda WHERE id_tienda = ?", String.class, tienda));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM usuario_rol ur JOIN rol r ON r.id_rol=ur.id_rol WHERE ur.id_usuario=? AND r.nombre='DUENO_TIENDA'", Integer.class, dueno));
    }

    @Test
    void catalogoPrivadoExigePropiedadEnTodasLasLecturas() throws Exception {
        UUID dueno = UUID.randomUUID(), ajeno = UUID.randomUUID(), admin = UUID.randomUUID(), tienda = UUID.randomUUID(), id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Dueño', ?), (?, 'Ajeno', ?), (?, 'Admin', ?)", dueno, dueno + "@example.test", ajeno, ajeno + "@example.test", admin, admin + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?, id_rol FROM rol WHERE nombre='ADMIN_GENERAL'", admin);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug) VALUES (?,?,'Tienda',?)", tienda, dueno, "t-" + tienda);
        var propietario = UsernamePasswordAuthenticationToken.authenticated(dueno, null, List.of());
        var otro = UsernamePasswordAuthenticationToken.authenticated(ajeno, null, List.of());
        var administrador = UsernamePasswordAuthenticationToken.authenticated(admin, null, List.of());
        String base = "/api/tiendas/" + tienda;
        var rutas = List.of("/categorias", "/categorias/" + id, "/productos", "/productos/" + id,
                "/productos/" + id + "/presentaciones", "/productos/" + id + "/presentaciones/" + id,
                "/productos/" + id + "/imagenes", "/productos/" + id + "/imagenes/" + id,
                "/productos/" + id + "/colecciones", "/colecciones", "/colecciones/activas", "/colecciones/" + id,
                "/colecciones/" + id + "/productos", "/variantes", "/variantes/" + id,
                "/variantes/" + id + "/valores", "/variantes/" + id + "/valores/" + id,
                "/presentaciones/" + id + "/imagenes", "/imagenes/" + id + "/presentaciones");
        for (String ruta : rutas) {
            mvc.perform(get(base + ruta)).andExpect(status().isUnauthorized());
            mvc.perform(get(base + ruta).with(authentication(otro))).andExpect(status().isForbidden());
        }
        mvc.perform(get(base + "/productos").with(authentication(propietario))).andExpect(status().isOk());
        mvc.perform(get(base + "/productos").with(authentication(administrador))).andExpect(status().isOk());
    }

    @Test
    void activacionExigePropietarioRolCsrfYEstadoAdmisible() throws Exception {
        UUID dueno = UUID.randomUUID(), ajeno = UUID.randomUUID(), sinRol = UUID.randomUUID();
        UUID tienda = UUID.randomUUID(), suspendida = UUID.randomUUID(), cerrada = UUID.randomUUID(), tiendaSinRol = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Dueño', ?), (?, 'Ajeno', ?), (?, 'Sin rol', ?)", dueno, dueno + "@example.test", ajeno, ajeno + "@example.test", sinRol, sinRol + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?, id_rol FROM rol WHERE nombre='DUENO_TIENDA'", dueno);
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?, id_rol FROM rol WHERE nombre='DUENO_TIENDA'", ajeno);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado,fecha_cierre) VALUES (?,?,'Borrador',?,'BORRADOR',NULL), (?,?,'Suspendida',?,'SUSPENDIDA',NULL), (?,?,'Cerrada',?,'CERRADA',now()), (?,?,'Sin rol',?,'BORRADOR',NULL)", tienda, dueno, "t-" + tienda, suspendida, dueno, "t-" + suspendida, cerrada, dueno, "t-" + cerrada, tiendaSinRol, sinRol, "t-" + tiendaSinRol);
        var propietario = UsernamePasswordAuthenticationToken.authenticated(dueno, null, List.of());
        var otro = UsernamePasswordAuthenticationToken.authenticated(ajeno, null, List.of());
        var sinPermiso = UsernamePasswordAuthenticationToken.authenticated(sinRol, null, List.of());
        String ruta = "/api/tiendas/" + tienda + "/activacion";
        mvc.perform(post(ruta).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post(ruta).with(authentication(propietario))).andExpect(status().isForbidden());
        mvc.perform(post(ruta).with(csrf()).with(authentication(otro))).andExpect(status().isForbidden());
        mvc.perform(post("/api/tiendas/" + tiendaSinRol + "/activacion").with(csrf()).with(authentication(sinPermiso))).andExpect(status().isForbidden());
        mvc.perform(post("/api/tiendas/" + suspendida + "/activacion").with(csrf()).with(authentication(propietario))).andExpect(status().isConflict());
        mvc.perform(post("/api/tiendas/" + cerrada + "/activacion").with(csrf()).with(authentication(propietario))).andExpect(status().isConflict());
        mvc.perform(post("/api/tiendas/" + UUID.randomUUID() + "/activacion").with(csrf()).with(authentication(propietario))).andExpect(status().isNotFound());
        mvc.perform(post(ruta).with(csrf()).with(authentication(propietario))).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACTIVA"));
        entityManager.flush();
        mvc.perform(post(ruta).with(csrf()).with(authentication(propietario))).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACTIVA"));
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVA", jdbc.queryForObject("SELECT estado FROM tienda WHERE id_tienda = ?", String.class, tienda));
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void activacionConcurrenteNoRevierteRetiroDeRol() throws Exception {
        UUID dueno = UUID.randomUUID(), tienda = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Dueño', ?)", dueno, dueno + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?, id_rol FROM rol WHERE nombre='DUENO_TIENDA'", dueno);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'BORRADOR')", tienda, dueno, "t-" + tienda);
        var ejecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var iniciada = new java.util.concurrent.CountDownLatch(1);
            var futuro = transacciones.execute(estado -> {
                jdbc.queryForList("SELECT id_usuario FROM usuario_plataforma WHERE id_usuario = ? FOR UPDATE", dueno);
                var activacion = ejecutor.submit(() -> {
                    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(dueno, null, List.of()));
                    iniciada.countDown();
                    try { tiendaService.activarTienda(tienda); }
                    finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
                });
                try {
                    org.junit.jupiter.api.Assertions.assertTrue(iniciada.await(5, java.util.concurrent.TimeUnit.SECONDS));
                    org.junit.jupiter.api.Assertions.assertThrows(java.util.concurrent.TimeoutException.class,
                            () -> activacion.get(300, java.util.concurrent.TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                usuarioService.retirarDueno(dueno);
                return activacion;
            });
            var error = org.junit.jupiter.api.Assertions.assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> futuro.get(10, java.util.concurrent.TimeUnit.SECONDS));
            org.junit.jupiter.api.Assertions.assertInstanceOf(org.springframework.security.access.AccessDeniedException.class, error.getCause());
            org.junit.jupiter.api.Assertions.assertEquals("SUSPENDIDA", jdbc.queryForObject("SELECT estado FROM tienda WHERE id_tienda = ?", String.class, tienda));
        } finally {
            ejecutor.shutdownNow();
            jdbc.update("DELETE FROM tienda WHERE id_tienda = ?", tienda);
            jdbc.update("DELETE FROM usuario_rol WHERE id_usuario = ?", dueno);
            jdbc.update("DELETE FROM usuario_plataforma WHERE id_usuario = ?", dueno);
        }
    }

    @Test
    void registroSesionCsrfPerfilYCierre() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombres\":\"Prueba\",\"correoElectronico\":\"sesion@example.test\",\"password\":\"password-seguro\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombres\":\"Prueba\",\"correoElectronico\":\"sesion@example.test\",\"password\":\"password-seguro\"}"))
                .andExpect(status().isCreated());
        var id = usuarios.findByEmail("sesion@example.test").orElseThrow().getIdUsuario();
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"sesion@example.test\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
        var login = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"sesion@example.test\",\"password\":\"password-seguro\"}"))
                .andExpect(status().isNoContent()).andReturn();
        var session = login.getResponse().getCookie("SESSION");
        org.junit.jupiter.api.Assertions.assertNotNull(session);
        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.correoElectronico").value("sesion@example.test"))
                .andExpect(jsonPath("$.roles[0]").value("CLIENTE"));
        mvc.perform(get("/api/usuarios/" + id).cookie(session)).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios/" + java.util.UUID.randomUUID()).cookie(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/tiendas").cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Tienda de prueba\"}")).andExpect(status().isCreated());
        entityManager.flush();
        UUID tienda = jdbc.queryForObject("SELECT id_tienda FROM tienda WHERE id_propietario = ?", UUID.class, id);
        assert jdbc.queryForObject("SELECT count(*) FROM configuracion_tienda WHERE id_tienda = ?", Integer.class, tienda) == 1;
        assert jdbc.queryForObject("SELECT count(*) FROM cuenta_pago_tienda WHERE id_tienda = ? AND estado = 'PENDIENTE'", Integer.class, tienda) == 1;
        String ruta = "/api/tiendas/" + tienda;
        mvc.perform(post(ruta + "/categorias").cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Prueba\",\"slug\":\"prueba\"}")).andExpect(status().isCreated());
        entityManager.flush();
        UUID categoria = jdbc.queryForObject("SELECT id_categoria FROM categoria WHERE id_tienda = ?", UUID.class, tienda);
        mvc.perform(post(ruta + "/productos").cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Producto\",\"idCategoria\":\"" + categoria + "\",\"precioBase\":15.50}"))
                .andExpect(status().isCreated());
        entityManager.flush();
        UUID producto = jdbc.queryForObject("SELECT id_producto FROM producto WHERE id_tienda = ?", UUID.class, tienda);
        mvc.perform(post(ruta + "/productos/" + producto + "/presentaciones").cookie(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"sku-prueba\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.precioEfectivo").value(15.5));
        UUID presentacion = jdbc.queryForObject("SELECT id_presentacion FROM presentacion_producto WHERE id_tienda = ?", UUID.class, tienda);
        mvc.perform(put(ruta + "/inventario/" + presentacion).cookie(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"stockActual\":3,\"stockMinimo\":1}"))
                .andExpect(status().isNoContent());
        mvc.perform(get(ruta + "/inventario").cookie(session).param("texto", "sku-prueba").param("alertaStock", "false").param("page", "0").param("size", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1)).andExpect(jsonPath("$.contenido[0].stockActual").value(3));
        mvc.perform(get(ruta + "/inventario").cookie(session).param("alertaStock", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(patch(ruta + "/configuracion").cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idPlantilla\":1,\"tituloInicio\":\"Mi tienda\",\"mostrarCarrusel\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tituloInicio").value("Mi tienda"));
        mvc.perform(get(ruta + "/cuenta-pago").cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        mvc.perform(post(ruta + "/cuenta-pago/mercado-pago/oauth/inicios").cookie(session).with(csrf()))
                .andExpect(status().isNotFound());
        // RN-M2-13 (Bloque 2): 422 con el requisito incumplido en lenguaje de usuario
        mvc.perform(patch(ruta + "/productos/" + producto + "/estado?estado=PUBLICADO").cookie(session).with(csrf()))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.campos.estado").value(com.ecommerce.backend.shared.exception.MensajesUsuario.PUBLICAR_TIENDA_INACTIVA));
        mvc.perform(post(ruta + "/activacion").cookie(session).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACTIVA"));
        entityManager.flush();
        mvc.perform(patch(ruta + "/productos/" + producto + "/estado?estado=PUBLICADO").cookie(session).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PUBLICADO"));
        mvc.perform(post("/logout").cookie(session).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}
