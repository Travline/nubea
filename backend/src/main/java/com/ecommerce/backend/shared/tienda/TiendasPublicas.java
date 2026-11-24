package com.ecommerce.backend.shared.tienda;

import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import com.ecommerce.backend.shared.exception.TiendaNoDisponibleException;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Resolución de tiendas por slug en rutas públicas y de comprador.
 * Distingue "no existe" (404 NO_ENCONTRADO) de "existe pero no está ACTIVA" (404 TIENDA_NO_DISPONIBLE),
 * sin devolver datos del catálogo en ningún caso.
 */
public final class TiendasPublicas {
    private TiendasPublicas() {
    }

    private record Fila(UUID id, String estado) {}

    /** Tienda ACTIVA: vitrina, carrito y checkout. */
    public static UUID activa(JdbcTemplate jdbc, String slug) {
        Fila fila = buscar(jdbc, slug);
        if (!"ACTIVA".equals(fila.estado())) throw new TiendaNoDisponibleException();
        return fila.id();
    }

    /** Tienda en cualquier estado: el comprador consulta sus pedidos y su pago aunque la tienda ya no esté disponible. */
    public static UUID existente(JdbcTemplate jdbc, String slug) {
        return buscar(jdbc, slug).id();
    }

    /** Excepción adecuada cuando una consulta filtrada por ACTIVA no encontró la tienda. */
    public static RuntimeException noActiva(JdbcTemplate jdbc, String slug) {
        try {
            buscar(jdbc, slug);
            return new TiendaNoDisponibleException();
        } catch (RecursoNoEncontradoException ex) {
            return ex;
        }
    }

    private static Fila buscar(JdbcTemplate jdbc, String slug) {
        return jdbc.query("SELECT id_tienda, estado FROM tienda WHERE slug = ?",
                        (rs, n) -> new Fila(rs.getObject(1, UUID.class), rs.getString(2)), slug)
                .stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("La tienda no existe"));
    }
}
