package com.ecommerce.backend.shared.storage;

import com.ecommerce.backend.exception.StorageException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AlmacenamientoImagenes {
    private final StoragePort storage;
    private final ImagenValidator validator;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    @Value("${storage.public-base-url}") private String publicBaseUrl;

    public String url(String key) {
        if (key == null || Arrays.stream(key.split("/", -1)).anyMatch(s -> s.isEmpty() || s.equals(".") || s.equals("..") || s.contains("\\")))
            throw new IllegalArgumentException("Clave de almacenamiento inválida");
        URI base = URI.create(publicBaseUrl);
        if (!base.isAbsolute() || base.getHost() == null || base.getQuery() != null || base.getFragment() != null
                || !("https".equals(base.getScheme()) || "http".equals(base.getScheme())))
            throw new IllegalStateException("URL pública de almacenamiento inválida");
        String encoded = Arrays.stream(key.split("/", -1))
                .map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
        return publicBaseUrl.replaceAll("/+$", "") + "/" + encoded;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public <T> T subirYGuardar(String prefijo, UUID tienda, MultipartFile archivo, Function<String, T> guardar) {
        if (prefijo == null || !prefijo.matches("[a-zA-Z0-9_-]+(?:/[a-zA-Z0-9_-]+)*"))
            throw new IllegalArgumentException("Prefijo de almacenamiento inválido");
        var imagen = validator.validarImagen(archivo);
        String key = prefijo + "/" + tienda + "/" + UUID.randomUUID() + imagen.extension();
        tx.executeWithoutResult(s -> jdbc.update("INSERT INTO storage_tarea(storage_key,tipo,disponible_desde) VALUES (?, 'SUBIDA', now() + interval '15 minutes')", key));
        try {
            try (var input = archivo.getInputStream()) {
                storage.subir(key, input, archivo.getSize(), imagen.mimeType());
            }
            return tx.execute(status -> {
                var rows = jdbc.queryForList("SELECT storage_key FROM storage_tarea WHERE storage_key=? AND tipo='SUBIDA' AND disponible_desde > now() FOR UPDATE", key);
                if (rows.isEmpty()) throw new IllegalStateException("La subida ha vencido");
                T resultado = guardar.apply(key);
                jdbc.update("DELETE FROM storage_tarea WHERE storage_key=?", key);
                return resultado;
            });
        } catch (IOException e) {
            solicitarLimpiezaFallida(key);
            throw new StorageException("No se pudo leer la imagen", e);
        } catch (RuntimeException e) {
            solicitarLimpiezaFallida(key);
            throw e;
        }
    }

    private void solicitarLimpiezaFallida(String key) {
        tx.executeWithoutResult(s -> jdbc.update("UPDATE storage_tarea SET tipo='ELIMINAR', disponible_desde=now() WHERE storage_key=?", key));
    }

    // Se llama dentro de la transacción que elimina o reemplaza la referencia.
    public void encolarEliminacion(String key) {
        jdbc.update("INSERT INTO storage_tarea(storage_key,tipo,disponible_desde) VALUES (?, 'ELIMINAR', now()) ON CONFLICT(storage_key) DO UPDATE SET tipo='ELIMINAR', disponible_desde=now()", key);
    }

    @Scheduled(fixedDelayString = "${storage.cleanup-delay-ms:60000}")
    public void procesarPendientes() {
        for (int i = 0; i < 50; i++) {
            String key = tx.execute(s -> {
                var keys = jdbc.queryForList("""
                    SELECT t.storage_key FROM storage_tarea t
                    WHERE t.disponible_desde <= now()
                      AND NOT EXISTS (SELECT 1 FROM banner_tienda b WHERE b.storage_key=t.storage_key)
                      AND NOT EXISTS (SELECT 1 FROM imagen_producto p WHERE p.storage_key=t.storage_key)
                    ORDER BY t.disponible_desde FOR UPDATE SKIP LOCKED LIMIT 1
                    """, String.class);
                if (keys.isEmpty()) return null;
                String next = keys.get(0);
                jdbc.update("UPDATE storage_tarea SET tipo='ELIMINAR', disponible_desde=now() + interval '5 minutes', intentos=intentos+1 WHERE storage_key=?", next);
                return next;
            });
            if (key == null) return;
            try {
                storage.eliminar(key);
                tx.executeWithoutResult(s -> jdbc.update("DELETE FROM storage_tarea WHERE storage_key=? AND tipo='ELIMINAR'", key));
            } catch (RuntimeException ignored) {
                // Se conserva el plazo de reintento; el adaptador registra el error.
            }
        }
    }
}
