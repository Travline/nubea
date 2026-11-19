package com.ecommerce.backend;

import static org.junit.jupiter.api.Assertions.*;
import com.ecommerce.backend.catalogo.dto.request.PresentacionProductoRequest;
import com.ecommerce.backend.catalogo.service.PresentacionProductoService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CatalogoIntegracionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PresentacionProductoService presentaciones;

    @Test
    void presentaPrecioInventarioYRechazaConjuntoDeVariantesDiferente() {
        UUID usuario = UUID.randomUUID(), tienda = UUID.randomUUID(), categoria = UUID.randomUUID(), producto = UUID.randomUUID();
        UUID color = UUID.randomUUID(), talla = UUID.randomUUID(), rojo = UUID.randomUUID(), grande = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Prueba', ?)", usuario, usuario + "@example.test");
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug) VALUES (?,?,'Tienda',?)", tienda, usuario, "t-" + tienda);
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Categoria','categoria')", categoria, tienda);
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base) VALUES (?,?,?,'Producto', 20)", producto, categoria, tienda);
        jdbc.update("INSERT INTO variante(id_variante,id_tienda,nombre) VALUES (?,?,'Color'),(?,?,'Talla')", color, tienda, talla, tienda);
        jdbc.update("INSERT INTO valor_variante(id_valor,id_variante,valor) VALUES (?,?,'Rojo'),(?,?,'Grande')", rojo, color, grande, talla);
        var request = new PresentacionProductoRequest();
        request.setSku("rojo"); request.setIdsValores(List.of(rojo));
        var creada = presentaciones.crearPresentacion(producto, tienda, request);
        assertEquals(0, creada.getPrecioEfectivo().compareTo(new BigDecimal("20")));
        assertEquals(0, jdbc.queryForObject("SELECT stock_actual FROM inventario WHERE id_presentacion = ?", Integer.class, creada.getIdPresentacion()));
        var otra = new PresentacionProductoRequest();
        otra.setSku("grande"); otra.setIdsValores(List.of(grande));
        assertThrows(IllegalArgumentException.class, () -> presentaciones.crearPresentacion(producto, tienda, otra));
    }
}
