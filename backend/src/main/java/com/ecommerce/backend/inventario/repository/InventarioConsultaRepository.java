package com.ecommerce.backend.inventario.repository;

import com.ecommerce.backend.inventario.dto.InventarioPage;
import com.ecommerce.backend.inventario.dto.InventarioResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventarioConsultaRepository {
    private final EntityManager entityManager;

    public InventarioPage consultar(UUID tienda, String texto, String estadoProducto, String estadoPresentacion,
                                    Boolean alerta, int page, int size) {
        StringBuilder from = new StringBuilder("""
            FROM presentacion_producto pp
            JOIN producto p ON p.id_producto = pp.id_producto AND p.id_tienda = pp.id_tienda
            LEFT JOIN inventario i ON i.id_presentacion = pp.id_presentacion
            WHERE pp.id_tienda = :tienda
            """);
        if (texto != null) from.append(" AND (lower(p.nombre) LIKE :texto ESCAPE '!' OR lower(pp.sku) LIKE :texto ESCAPE '!')");
        if (estadoProducto != null) from.append(" AND p.estado = :estadoProducto");
        if (estadoPresentacion != null) from.append(" AND pp.estado = :estadoPresentacion");
        if (alerta != null) from.append(alerta ? " AND i.stock_actual <= i.stock_minimo" : " AND i.stock_actual > i.stock_minimo");
        Query count = entityManager.createNativeQuery("SELECT count(*) " + from);
        Query rows = entityManager.createNativeQuery("SELECT p.id_producto, p.nombre, p.estado, pp.id_presentacion, pp.sku, pp.estado, i.stock_actual, i.stock_minimo, i.fecha_actualizacion " + from + " ORDER BY p.nombre, pp.sku, pp.id_presentacion");
        for (Query query : List.of(count, rows)) {
            query.setParameter("tienda", tienda);
            if (texto != null) query.setParameter("texto", "%" + texto.toLowerCase(java.util.Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
            if (estadoProducto != null) query.setParameter("estadoProducto", estadoProducto);
            if (estadoPresentacion != null) query.setParameter("estadoPresentacion", estadoPresentacion);
        }
        long total = ((Number) count.getSingleResult()).longValue();
        rows.setFirstResult(Math.toIntExact((long) page * size));
        rows.setMaxResults(size);
        @SuppressWarnings("unchecked")
        List<Object[]> results = rows.getResultList();
        List<InventarioResponse> contenido = results.stream().map(r -> new InventarioResponse(
                (UUID) r[0], (String) r[1], (String) r[2], (UUID) r[3], (String) r[4], (String) r[5],
                (Integer) r[6], (Integer) r[7], r[8] == null ? null : ((java.time.Instant) r[8]).atOffset(java.time.ZoneOffset.UTC), r[6] == null)).toList();
        return new InventarioPage(contenido, page, size, total, (int) Math.min(Integer.MAX_VALUE, (total + size - 1) / size));
    }
}
