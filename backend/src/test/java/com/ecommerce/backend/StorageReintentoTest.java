package com.ecommerce.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ecommerce.backend.shared.storage.AlmacenamientoImagenes;
import com.ecommerce.backend.shared.storage.StoragePort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "STORAGE_IT", matches = "true")
class StorageReintentoTest {
    @Autowired AlmacenamientoImagenes imagenes;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean StoragePort storage;

    @Test
    void fallosYProcesosInterrumpidosSeReintentan() {
        String key = "productos/test/" + UUID.randomUUID() + ".png";
        jdbc.update("INSERT INTO storage_tarea(storage_key,tipo,disponible_desde) VALUES (?, 'SUBIDA', now() - interval '1 minute')", key);
        doThrow(new IllegalStateException("S3 no disponible")).doNothing().when(storage).eliminar(key);
        try {
            imagenes.procesarPendientes();
            assertEquals(1, jdbc.queryForObject("SELECT intentos FROM storage_tarea WHERE storage_key=?", Integer.class, key));
            imagenes.procesarPendientes();
            verify(storage, times(1)).eliminar(key);
            jdbc.update("UPDATE storage_tarea SET disponible_desde=now()-interval '1 second' WHERE storage_key=?", key);
            imagenes.procesarPendientes();
            verify(storage, times(2)).eliminar(key);
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM storage_tarea WHERE storage_key=?", Integer.class, key));
        } finally {
            jdbc.update("DELETE FROM storage_tarea WHERE storage_key=?", key);
        }
    }
}
