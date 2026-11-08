package com.ecommerce.backend.inventario;

import static org.junit.jupiter.api.Assertions.*;
import com.ecommerce.backend.inventario.repository.InventarioConsultaRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "INVENTARIO_PG_TEST", matches = "true")
class InventarioPostgresTest {
    @Autowired EntityManager em;
    @Autowired InventarioConsultaRepository repository;

    @Test
    void filtraPaginaYAislaTiendasSinModificarInventario() {
        em.createNativeQuery("CREATE TEMP TABLE producto (id_producto uuid, id_tienda uuid, nombre text, estado text) ON COMMIT DROP").executeUpdate();
        em.createNativeQuery("CREATE TEMP TABLE presentacion_producto (id_presentacion uuid, id_producto uuid, id_tienda uuid, sku text, estado text) ON COMMIT DROP").executeUpdate();
        em.createNativeQuery("CREATE TEMP TABLE inventario (id_presentacion uuid, stock_actual integer, stock_minimo integer, fecha_actualizacion timestamptz) ON COMMIT DROP").executeUpdate();
        UUID tienda = UUID.randomUUID(), otra = UUID.randomUUID(), producto = UUID.randomUUID(), productoOtro = UUID.randomUUID();
        UUID baja = UUID.randomUUID(), sinRegistro = UUID.randomUUID(), ajena = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO producto VALUES (:p,:t,'Camisa','PUBLICADO'),(:o,:ot,'Ajeno','BORRADOR')").setParameter("p", producto).setParameter("t", tienda).setParameter("o", productoOtro).setParameter("ot", otra).executeUpdate();
        em.createNativeQuery("INSERT INTO presentacion_producto VALUES (:a,:p,:t,'SKU-1','ACTIVA'),(:b,:p,:t,'SKU-2','INACTIVA'),(:c,:o,:ot,'SKU-3','ACTIVA')").setParameter("a", baja).setParameter("b", sinRegistro).setParameter("c", ajena).setParameter("p", producto).setParameter("t", tienda).setParameter("o", productoOtro).setParameter("ot", otra).executeUpdate();
        em.createNativeQuery("INSERT INTO inventario VALUES (:a,2,2,now()),(:c,10,1,now())").setParameter("a", baja).setParameter("c", ajena).executeUpdate();
        var todos = repository.consultar(tienda, null, null, null, null, 0, 20);
        assertEquals(2, todos.totalElementos());
        assertNotNull(todos.contenido().get(0).fechaActualizacion());
        assertTrue(todos.contenido().get(1).sinRegistroInventario());
        assertNull(todos.contenido().get(1).stockActual());
        assertEquals(1, repository.consultar(tienda, "sku-1", "PUBLICADO", "ACTIVA", true, 0, 20).totalElementos());
        assertEquals(0, repository.consultar(tienda, null, null, null, false, 0, 20).totalElementos());
        assertEquals(1, repository.consultar(tienda, null, null, null, null, 1, 1).contenido().size());
        assertEquals(2L, ((Number) em.createNativeQuery("SELECT count(*) FROM inventario").getSingleResult()).longValue());
    }
}
