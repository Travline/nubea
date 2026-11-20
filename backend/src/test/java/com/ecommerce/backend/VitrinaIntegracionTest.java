package com.ecommerce.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import com.ecommerce.backend.vitrina.VitrinaController;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VitrinaIntegracionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired VitrinaController vitrina;

    @Test
    void filtraPorTiendaEstadoYDisponibilidad() throws Exception {
        UUID usuario = UUID.randomUUID(), tienda = UUID.randomUUID(), otra = UUID.randomUUID(), tercera = UUID.randomUUID();
        UUID categoria = UUID.randomUUID(), oculta = UUID.randomUUID(), producto = UUID.randomUUID(), borrador = UUID.randomUUID();
        UUID presentacion = UUID.randomUUID(), inactiva = UUID.randomUUID();
        String slug = "t-" + tienda;
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Prueba', ?)", usuario, usuario + "@example.test");
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Visible',?,'ACTIVA'),(?,?,'Otra',?,'BORRADOR')", tienda, usuario, slug, otra, usuario, "t-" + otra);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tercera',?,'ACTIVA')", tercera, usuario, "t-" + tercera);
        jdbc.update("INSERT INTO configuracion_tienda(id_tienda,id_plantilla) VALUES (?,1),(?,1),(?,1)", tienda, otra, tercera);
        UUID categoriaOtra = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Ajena','ajena')", categoriaOtra, tercera);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base,estado) VALUES (?,?,?,'Producto ajeno',100,'PUBLICADO')", UUID.randomUUID(), categoriaOtra, tercera);
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug,activa) VALUES (?,?,'Visible','visible',true),(?,?,'Oculta','oculta',false)", categoria, tienda, oculta, tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base,estado) VALUES (?,?,?,'Publicado',20,'PUBLICADO'),(?,?,?,'Borrador',10,'BORRADOR'),(?,?,?,'Oculto',10,'PUBLICADO')", producto, categoria, tienda, borrador, categoria, tienda, UUID.randomUUID(), oculta, tienda);
        jdbc.update("INSERT INTO presentacion_producto(id_presentacion,id_producto,id_tienda,sku,precio_override,estado,firma_valores) VALUES (?,?,?,'ACTIVO',15,'ACTIVA','a'),(?,?,?,'INACTIVO',10,'INACTIVA','b')", presentacion, producto, tienda, inactiva, producto, tienda);
        jdbc.update("INSERT INTO inventario(id_presentacion,stock_actual) VALUES (?,0)", presentacion);
        UUID coleccion = UUID.randomUUID(), coleccionOculta = UUID.randomUUID(), coleccionAjena = UUID.randomUUID();
        jdbc.update("INSERT INTO coleccion(id_coleccion,id_tienda,nombre,slug,activa) VALUES (?,?,'Visible','visible',true),(?,?,'Oculta','oculta',false),(?,?,'Ajena','ajena',true)", coleccion, tienda, coleccionOculta, tienda, coleccionAjena, tercera);
        jdbc.update("INSERT INTO coleccion_producto(id_coleccion,id_producto) VALUES (?,?),(?,?),(?,?)", coleccion, producto, coleccion, borrador, coleccionOculta, producto);
        for (String ruta : new String[]{"/api/publico/tiendas/", "/api/tiendas/"}) {
            mvc.perform(get(ruta + slug)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Visible"))
                .andExpect(jsonPath("$.colecciones.length()").value(1))
                .andExpect(jsonPath("$.colecciones[0].id").value(coleccion.toString()))
                .andExpect(jsonPath("$.colecciones[0].productos.length()").value(1))
                .andExpect(jsonPath("$.colecciones[0].productos[0]").value(producto.toString()))
                .andExpect(jsonPath("$.idPropietario").doesNotExist())
                .andExpect(jsonPath("$.productos[0].presentaciones[0].stockActual").doesNotExist())
                .andExpect(jsonPath("$.productos[0].presentaciones[0].sku").doesNotExist());
            mvc.perform(get(ruta + "t-" + otra)).andExpect(status().isNotFound());
        }
        var resultado = vitrina.obtener(slug);
        assertEquals(1, resultado.categorias().size());
        assertEquals(1, resultado.productos().size());
        var p = resultado.productos().getFirst();
        assertEquals(producto, p.id());
        assertEquals(1, p.presentaciones().size());
        assertEquals(0, p.presentaciones().getFirst().precio().compareTo(new BigDecimal("15")));
        assertFalse(p.presentaciones().getFirst().disponible());
        jdbc.update("UPDATE inventario SET stock_actual=2 WHERE id_presentacion=?", presentacion);
        assertTrue(vitrina.obtener(slug).productos().getFirst().presentaciones().getFirst().disponible());
        assertThrows(RecursoNoEncontradoException.class, () -> vitrina.obtener("t-" + otra));
        assertThrows(RecursoNoEncontradoException.class, () -> vitrina.obtener("inexistente"));
    }
}
