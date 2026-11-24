package com.ecommerce.backend;

import static org.junit.jupiter.api.Assertions.*;
import com.ecommerce.backend.shared.storage.AlmacenamientoImagenes;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.List;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "STORAGE_IT", matches = "true")
class StorageIntegracionTest {
    @Autowired AlmacenamientoImagenes imagenes;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    @Autowired S3Client s3;
    @Autowired MockMvc mvc;
    private final String bucket = "nubea-test";
    private MockMultipartFile archivo() {
        return new MockMultipartFile("archivo", "imagen.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0, 0, 0, 0});
    }

    @BeforeEach
    void bucket() {
        try { s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build()); }
        catch (Exception ex) { s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build()); }
        s3.putBucketPolicy(PutBucketPolicyRequest.builder().bucket(bucket).policy("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"s3:GetObject\",\"Resource\":[\"arn:aws:s3:::nubea-test/banners/*\",\"arn:aws:s3:::nubea-test/productos/*\"]}]}").build());
    }

    @Test
    void subidaYEliminacionDurable() throws Exception {
        UUID usuario = UUID.randomUUID(), tienda = UUID.randomUUID(), banner = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Test', ?)", usuario, usuario + "@example.test");
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug) VALUES (?,?,'Tienda',?)", tienda, usuario, "t-" + tienda);
        try {
            String key = imagenes.subirYGuardar("banners", tienda, archivo(), k -> {
                jdbc.update("INSERT INTO banner_tienda(id_banner,id_tienda,storage_key,orden) VALUES (?,?,?,1)", banner, tienda, k);
                return k;
            });
            assertNotNull(s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()));
            assertTrue(imagenes.url(key).endsWith("/" + key));
            var response = java.net.http.HttpClient.newHttpClient().send(
                    java.net.http.HttpRequest.newBuilder(java.net.URI.create(imagenes.url(key))).GET().build(),
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, response.statusCode());
            imagenes.encolarEliminacion(key);
            imagenes.procesarPendientes();
            assertNotNull(s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()));
            tx.executeWithoutResult(s -> {
                jdbc.update("DELETE FROM banner_tienda WHERE id_banner=?", banner);
                imagenes.encolarEliminacion(key);
            });
            imagenes.procesarPendientes();
            assertThrows(Exception.class, () -> s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM storage_tarea WHERE storage_key=?", Integer.class, key));
        } finally {
            jdbc.update("DELETE FROM tienda WHERE id_tienda=?", tienda);
            jdbc.update("DELETE FROM usuario_plataforma WHERE id_usuario=?", usuario);
        }
    }

    @Test
    void falloDePersistenciaDejaTareaQuePuedeReintentarse() {
        var key = new AtomicReference<String>();
        assertThrows(IllegalStateException.class, () -> imagenes.subirYGuardar("productos", UUID.randomUUID(), archivo(), k -> {
            key.set(k);
            throw new IllegalStateException("Fallo de persistencia");
        }));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM storage_tarea WHERE storage_key=?", Integer.class, key.get()));
        imagenes.procesarPendientes();
        assertThrows(Exception.class, () -> s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key.get()).build()));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM storage_tarea WHERE storage_key=?", Integer.class, key.get()));
    }

    @Test
    void endpointsDeProductoYBannerGestionanArchivosYPermisos() throws Exception {
        UUID usuario = UUID.randomUUID(), ajeno = UUID.randomUUID(), tienda = UUID.randomUUID();
        UUID categoria = UUID.randomUUID(), producto = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Test', ?), (?, 'Ajeno', ?)", usuario, usuario + "@example.test", ajeno, ajeno + "@example.test");
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug) VALUES (?,?,'Tienda',?)", tienda, usuario, "t-" + tienda);
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='DUENO_TIENDA'", usuario);
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Categoria','categoria')", categoria, tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base) VALUES (?,?,?,'Producto',10)", producto, categoria, tienda);
        var owner = UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of());
        var other = UsernamePasswordAuthenticationToken.authenticated(ajeno, null, List.of());
        String ruta = "/api/tiendas/" + tienda + "/productos/" + producto + "/imagenes";
        String banners = "/api/tiendas/" + tienda + "/banners";
        try {
            mvc.perform(multipart(ruta).file(archivo()).param("orden", "1").with(csrf()).with(authentication(other)))
                    .andExpect(status().isForbidden());
            mvc.perform(multipart(ruta).file(archivo()).param("orden", "1").param("esPrincipal", "true").with(csrf()).with(authentication(owner)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.url").exists()).andExpect(jsonPath("$.storageKey").doesNotExist());
            UUID id = jdbc.queryForObject("SELECT id_imagen FROM imagen_producto WHERE id_producto=?", UUID.class, producto);
            String anterior = jdbc.queryForObject("SELECT storage_key FROM imagen_producto WHERE id_imagen=?", String.class, id);
            mvc.perform(multipart(ruta + "/" + id + "/archivo").file(archivo()).with(r -> { r.setMethod("PUT"); return r; }).with(csrf()).with(authentication(owner)))
                    .andExpect(status().isOk());
            String actual = jdbc.queryForObject("SELECT storage_key FROM imagen_producto WHERE id_imagen=?", String.class, id);
            assertNotEquals(anterior, actual);
            mvc.perform(delete(ruta + "/" + id).with(csrf()).with(authentication(owner))).andExpect(status().isNoContent());
            mvc.perform(multipart(banners).file(archivo()).param("orden", "1").with(csrf()).with(authentication(owner)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.url").exists()).andExpect(jsonPath("$.storageKey").doesNotExist());
            UUID banner = jdbc.queryForObject("SELECT id_banner FROM banner_tienda WHERE id_tienda=?", UUID.class, tienda);
            mvc.perform(multipart(banners).file(archivo()).param("orden", "2").with(csrf()).with(authentication(owner)))
                    .andExpect(status().isCreated());
            UUID segundo = jdbc.queryForObject("SELECT id_banner FROM banner_tienda WHERE id_tienda=? AND orden=2", UUID.class, tienda);
            mvc.perform(multipart(banners + "/" + banner + "/archivo").file(archivo())
                    .with(r -> { r.setMethod("PUT"); return r; }).with(csrf()).with(authentication(owner)))
                    .andExpect(status().isOk());
            mvc.perform(put(banners + "/orden").with(csrf()).with(authentication(owner)).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content("{\"banners\":[{\"idBanner\":\"" + segundo + "\",\"orden\":1},{\"idBanner\":\"" + banner + "\",\"orden\":2}]}"))
                    .andExpect(status().isOk());
            assertEquals(1, jdbc.queryForObject("SELECT orden FROM banner_tienda WHERE id_banner=?", Integer.class, segundo));
            mvc.perform(delete(banners + "/" + banner).with(csrf()).with(authentication(owner))).andExpect(status().isNoContent());
            mvc.perform(delete(banners + "/" + segundo).with(csrf()).with(authentication(owner))).andExpect(status().isNoContent());
            imagenes.procesarPendientes();
            assertThrows(Exception.class, () -> s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(anterior).build()));
            assertThrows(Exception.class, () -> s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(actual).build()));
        } finally {
            jdbc.update("DELETE FROM producto WHERE id_producto=?", producto);
            jdbc.update("DELETE FROM categoria WHERE id_categoria=?", categoria);
            jdbc.update("DELETE FROM tienda WHERE id_tienda=?", tienda);
            jdbc.update("DELETE FROM usuario_rol WHERE id_usuario=?", usuario);
            jdbc.update("DELETE FROM usuario_plataforma WHERE id_usuario IN (?,?)", usuario, ajeno);
        }
    }
}
