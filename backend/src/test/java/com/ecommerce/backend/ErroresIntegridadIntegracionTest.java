package com.ecommerce.backend;

import static com.ecommerce.backend.shared.exception.MensajesUsuario.*;
import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.backend.catalogo.model.Categoria;
import com.ecommerce.backend.catalogo.repository.CategoriaRepository;
import com.ecommerce.backend.shared.exception.ErrorResponse;
import com.ecommerce.backend.shared.exception.GlobalExceptionHandler;
import com.ecommerce.backend.shared.exception.RestriccionesBd;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bloque 1: provoca violaciones reales de restricciones en PostgreSQL y comprueba que
 * GlobalExceptionHandler responde con código, campo y mensaje para el usuario.
 * No es @Transactional: cada violación aborta su propia transacción; los datos se limpian al final.
 */
@SpringBootTest
class ErroresIntegridadIntegracionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    @Autowired GlobalExceptionHandler handler;
    @Autowired CategoriaRepository categorias;

    private UUID usuario, tienda, categoria, producto, variante, presentacion;
    private String slugTienda;

    @BeforeEach
    void preparar() {
        usuario = UUID.randomUUID(); tienda = UUID.randomUUID(); categoria = UUID.randomUUID();
        producto = UUID.randomUUID(); variante = UUID.randomUUID(); presentacion = UUID.randomUUID();
        slugTienda = "t-" + tienda;
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Dueño', ?)", usuario, usuario + "@example.test");
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug) VALUES (?,?,'Tienda',?)", tienda, usuario, slugTienda);
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Ropa','ropa')", categoria, tienda);
        jdbc.update("INSERT INTO coleccion(id_tienda,nombre,slug) VALUES (?,'Verano','verano')", tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base) VALUES (?,?,?,'Polo',10)", producto, categoria, tienda);
        jdbc.update("INSERT INTO variante(id_variante,id_tienda,nombre) VALUES (?,?,'Color')", variante, tienda);
        jdbc.update("INSERT INTO valor_variante(id_variante,valor) VALUES (?,'Rojo')", variante);
        jdbc.update("INSERT INTO presentacion_producto(id_presentacion,id_producto,id_tienda,sku) VALUES (?,?,?,'POLO-1')", presentacion, producto, tienda);
        jdbc.update("INSERT INTO inventario(id_presentacion) VALUES (?)", presentacion);
        jdbc.update("INSERT INTO imagen_producto(id_producto,storage_key,es_principal) VALUES (?,?,true)", producto, "test/" + UUID.randomUUID());
        jdbc.update("INSERT INTO banner_tienda(id_tienda,storage_key,orden) VALUES (?,?,1)", tienda, "test/" + UUID.randomUUID());
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM banner_tienda WHERE id_tienda=?", tienda);
        jdbc.update("DELETE FROM imagen_producto WHERE id_producto IN (SELECT id_producto FROM producto WHERE id_tienda=?)", tienda);
        jdbc.update("DELETE FROM inventario WHERE id_presentacion IN (SELECT id_presentacion FROM presentacion_producto WHERE id_tienda=?)", tienda);
        jdbc.update("DELETE FROM presentacion_producto WHERE id_tienda=?", tienda);
        jdbc.update("DELETE FROM producto WHERE id_tienda=?", tienda);
        jdbc.update("DELETE FROM valor_variante WHERE id_variante IN (SELECT id_variante FROM variante WHERE id_tienda=?)", tienda);
        jdbc.update("DELETE FROM variante WHERE id_tienda=?", tienda);
        jdbc.update("DELETE FROM coleccion WHERE id_tienda=?", tienda);
        jdbc.update("DELETE FROM categoria WHERE id_tienda=?", tienda);
        jdbc.update("DELETE FROM tienda WHERE id_propietario=?", usuario);
        jdbc.update("DELETE FROM usuario_plataforma WHERE id_usuario=?", usuario);
    }

    /** Ejecuta la escritura en su propia transacción y devuelve la respuesta del handler. */
    private ResponseEntity<ErrorResponse> violar(Consumer<JdbcTemplate> escritura) {
        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                () -> tx.executeWithoutResult(estado -> escritura.accept(jdbc)));
        return handler.integridad(error);
    }

    private void comprobar(ResponseEntity<ErrorResponse> respuesta, int codigo, String campo, String mensaje) {
        ErrorResponse cuerpo = respuesta.getBody();
        assertNotNull(cuerpo);
        assertEquals(codigo, respuesta.getStatusCode().value(), () -> "Respuesta: " + cuerpo);
        assertEquals(codigo == 409 ? "DUPLICADO" : "DATOS_INVALIDOS", cuerpo.error());
        assertEquals(mensaje, cuerpo.mensaje());
        assertEquals(Map.of(campo, mensaje), cuerpo.campos());
    }

    @Test
    void restriccionesUnicasResponden409ConCampo() {
        comprobar(violar(j -> j.update("INSERT INTO tienda(id_propietario,nombre,slug) VALUES (?,'Otra',?)", usuario, slugTienda)),
                409, "slug", TIENDA_DIRECCION_DUPLICADA);
        comprobar(violar(j -> j.update("INSERT INTO categoria(id_tienda,nombre,slug) VALUES (?,'Otra','ropa')", tienda)),
                409, "slug", CATEGORIA_DIRECCION_DUPLICADA);
        comprobar(violar(j -> j.update("INSERT INTO coleccion(id_tienda,nombre,slug) VALUES (?,'Otra','verano')", tienda)),
                409, "slug", COLECCION_DIRECCION_DUPLICADA);
        comprobar(violar(j -> j.update("INSERT INTO variante(id_tienda,nombre) VALUES (?,' color ')", tienda)),
                409, "nombre", OPCION_DUPLICADA);
        comprobar(violar(j -> j.update("INSERT INTO valor_variante(id_variante,valor) VALUES (?,'ROJO')", variante)),
                409, "valor", VALOR_DUPLICADO);
        comprobar(violar(j -> j.update("INSERT INTO presentacion_producto(id_producto,id_tienda,sku,firma_valores) VALUES (?,?,'POLO-1','x')", producto, tienda)),
                409, "sku", CODIGO_DUPLICADO);
        comprobar(violar(j -> j.update("INSERT INTO presentacion_producto(id_producto,id_tienda,sku) VALUES (?,?,'POLO-2')", producto, tienda)),
                409, "idsValores", COMBINACION_REPETIDA);
        comprobar(violar(j -> j.update("INSERT INTO imagen_producto(id_producto,storage_key,es_principal) VALUES (?,?,true)", producto, "test/" + UUID.randomUUID())),
                409, "esPrincipal", FOTO_PRINCIPAL_DUPLICADA);
        comprobar(violar(j -> {
                    j.execute("SET CONSTRAINTS uq_banner_tienda_orden IMMEDIATE"); // es diferible: forzar la comprobación
                    j.update("INSERT INTO banner_tienda(id_tienda,storage_key,orden) VALUES (?,?,1)", tienda, "test/" + UUID.randomUUID());
                }), 409, "orden", POSICION_BANNER_OCUPADA);
    }

    @Test
    void restriccionesCheckResponden400ConCampo() {
        comprobar(violar(j -> j.update("INSERT INTO categoria(id_tienda,nombre,slug) VALUES (?,'Otra','Mal Slug')", tienda)),
                400, "slug", DIRECCION_FORMATO);
        comprobar(violar(j -> j.update("INSERT INTO coleccion(id_tienda,nombre,slug) VALUES (?,'Otra','con_guion_bajo')", tienda)),
                400, "slug", DIRECCION_FORMATO);
        comprobar(violar(j -> j.update("INSERT INTO categoria(id_tienda,nombre,slug) VALUES (?,'   ','vacia')", tienda)),
                400, "nombre", NOMBRE_VACIO);
        comprobar(violar(j -> j.update("INSERT INTO presentacion_producto(id_producto,id_tienda,sku,firma_valores) VALUES (?,?,'  ','y')", producto, tienda)),
                400, "sku", CODIGO_VACIO);
        comprobar(violar(j -> j.update("UPDATE producto SET precio_base = -1 WHERE id_producto=?", producto)),
                400, "precioBase", PRECIO_NEGATIVO);
        comprobar(violar(j -> j.update("UPDATE presentacion_producto SET precio_override = -5 WHERE id_presentacion=?", presentacion)),
                400, "precioOverride", PRECIO_NEGATIVO);
        comprobar(violar(j -> j.update("UPDATE inventario SET stock_actual = -1 WHERE id_presentacion=?", presentacion)),
                400, "stockActual", UNIDADES_NEGATIVAS);
        comprobar(violar(j -> j.update("UPDATE inventario SET stock_minimo = -1 WHERE id_presentacion=?", presentacion)),
                400, "stockMinimo", UNIDADES_NEGATIVAS);
    }

    @Test
    void conJpaSeUsaElNombreDeRestriccionDeHibernate() {
        Categoria duplicada = new Categoria();
        duplicada.setIdTienda(tienda);
        duplicada.setNombre("Otra");
        duplicada.setSlug("ropa");
        duplicada.setActiva(true);
        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class, () -> categorias.saveAndFlush(duplicada));

        boolean hayExcepcionDeHibernate = false;
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof org.hibernate.exception.ConstraintViolationException cve) {
                hayExcepcionDeHibernate = true;
                assertEquals("categoria_id_tienda_slug_key", cve.getConstraintName());
            }
        }
        assertTrue(hayExcepcionDeHibernate, "Se esperaba ConstraintViolationException de Hibernate en la cadena de causas");
        assertEquals("categoria_id_tienda_slug_key", RestriccionesBd.detectar(error).restriccion());
        assertEquals(RestriccionesBd.Tipo.UNICA, RestriccionesBd.detectar(error).tipo());
        comprobar(handler.integridad(error), 409, "slug", CATEGORIA_DIRECCION_DUPLICADA);
    }

    @Test
    void restriccionSinTraduccionMantieneElMensajeGenerico() {
        ResponseEntity<ErrorResponse> respuesta = violar(j -> j.update(
                "INSERT INTO producto(id_categoria,id_tienda,nombre,precio_base) VALUES (?,?,'X',1)", UUID.randomUUID(), tienda));
        assertEquals(409, respuesta.getStatusCode().value());
        assertEquals("INTEGRIDAD", respuesta.getBody().error());
        assertNull(respuesta.getBody().campos());
    }
}
