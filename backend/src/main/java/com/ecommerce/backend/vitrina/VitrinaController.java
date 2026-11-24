package com.ecommerce.backend.vitrina;

import com.ecommerce.backend.shared.tienda.TiendasPublicas;

import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import com.ecommerce.backend.shared.storage.AlmacenamientoImagenes;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class VitrinaController {
    private final JdbcTemplate jdbc;
    private final AlmacenamientoImagenes imagenes;

    public record Banner(String url, String enlace) {}
    public record Categoria(UUID id, String nombre, String slug) {}
    public record Coleccion(UUID id, String nombre, String slug, List<UUID> productos) {}
    public record Opcion(String nombre, String valor) {}
    /**
     * {@code precioAnterior}: precio base del producto cuando la versión tiene un precio especial menor (para mostrarlo
     * tachado); null en otro caso. {@code disponible} se mantiene por compatibilidad.
     * {@code disponibilidad}: DISPONIBLE | ULTIMAS_UNIDADES | AGOTADO. {@code unidadesRestantes} solo aparece con
     * ULTIMAS_UNIDADES; en los demás casos no se expone el stock.
     */
    public record Presentacion(UUID id, BigDecimal precio, BigDecimal precioAnterior, boolean disponible, List<Opcion> opciones,
                               String disponibilidad,
                               @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                               Integer unidadesRestantes) {}

    /** Umbral de "últimas unidades": el stock mínimo de la versión si es mayor que 0; si no, 5. */
    static final int UMBRAL_POR_DEFECTO = 5;

    static String disponibilidad(int stock, int stockMinimo) {
        if (stock <= 0) return "AGOTADO";
        int umbral = stockMinimo > 0 ? stockMinimo : UMBRAL_POR_DEFECTO;
        return stock <= umbral ? "ULTIMAS_UNIDADES" : "DISPONIBLE";
    }
    public record Producto(UUID id, UUID categoriaId, String nombre, String descripcion, List<String> imagenes, List<Presentacion> presentaciones) {}
    public record Vitrina(String nombre, String slug, String plantilla, String titulo, String descripcion, boolean mostrarCarrusel,
                          List<Banner> banners, List<Categoria> categorias, List<Producto> productos, List<Coleccion> colecciones) {}
    private record Cabecera(UUID id, String nombre, String slug, String plantilla, String titulo, String descripcion, boolean mostrarCarrusel) {}

    @GetMapping({"/api/publico/tiendas/{slug}", "/api/tiendas/{slug}"})
    @Transactional(readOnly = true)
    public Vitrina obtener(@PathVariable String slug) {
        var tiendas = jdbc.query("""
            SELECT t.id_tienda, t.nombre, t.slug, p.codigo, c.titulo_inicio, c.descripcion_inicio, c.mostrar_carrusel
            FROM tienda t JOIN configuracion_tienda c ON c.id_tienda=t.id_tienda
            JOIN plantilla p ON p.id_plantilla=c.id_plantilla WHERE t.slug=? AND t.estado='ACTIVA'
            """, (rs, n) -> new Cabecera(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getBoolean(7)), slug);
        if (tiendas.isEmpty()) throw TiendasPublicas.noActiva(jdbc, slug);
        var t = tiendas.getFirst();
        var banners = jdbc.query("SELECT storage_key, enlace_destino FROM banner_tienda WHERE id_tienda=? AND activo=true ORDER BY orden", (rs, n) -> new Banner(imagenes.url(rs.getString(1)), rs.getString(2)), t.id());
        var categorias = jdbc.query("SELECT id_categoria, nombre, slug FROM categoria WHERE id_tienda=? AND activa=true ORDER BY nombre", (rs, n) -> new Categoria(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)), t.id());
        var productos = jdbc.query("""
            SELECT p.id_producto, p.id_categoria, p.nombre, p.descripcion FROM producto p
            JOIN categoria c ON c.id_categoria=p.id_categoria AND c.id_tienda=p.id_tienda
            WHERE p.id_tienda=? AND p.estado='PUBLICADO' AND c.activa=true ORDER BY p.nombre, p.id_producto
            """, (rs, n) -> producto(t.id(), rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), rs.getString(4)), t.id());
        var colecciones = jdbc.query("SELECT id_coleccion, nombre, slug FROM coleccion WHERE id_tienda=? AND activa=true ORDER BY nombre, id_coleccion", (rs, n) -> {
            UUID id = rs.getObject(1, UUID.class);
            var miembros = jdbc.query("""
                SELECT p.id_producto FROM coleccion_producto cp
                JOIN producto p ON p.id_producto=cp.id_producto AND p.id_tienda=?
                JOIN categoria c ON c.id_categoria=p.id_categoria AND c.id_tienda=p.id_tienda
                WHERE cp.id_coleccion=? AND p.estado='PUBLICADO' AND c.activa=true ORDER BY p.nombre,p.id_producto
                """, (r, i) -> r.getObject(1, UUID.class), t.id(), id);
            return new Coleccion(id, rs.getString(2), rs.getString(3), miembros);
        }, t.id());
        return new Vitrina(t.nombre(), t.slug(), t.plantilla(), t.titulo(), t.descripcion(), t.mostrarCarrusel(), banners, categorias, productos, colecciones);
    }

    private Producto producto(UUID tienda, UUID id, UUID categoria, String nombre, String descripcion) {
        var fotos = jdbc.query("SELECT storage_key FROM imagen_producto WHERE id_producto=? ORDER BY es_principal DESC, orden", (rs, n) -> imagenes.url(rs.getString(1)), id);
        var presentaciones = jdbc.query("""
            SELECT pr.id_presentacion, COALESCE(pr.precio_override,p.precio_base), COALESCE(i.stock_actual,0), COALESCE(i.stock_minimo,0),
                   CASE WHEN pr.precio_override IS NOT NULL AND pr.precio_override < p.precio_base THEN p.precio_base END
            FROM presentacion_producto pr JOIN producto p ON p.id_producto=pr.id_producto AND p.id_tienda=pr.id_tienda
            LEFT JOIN inventario i ON i.id_presentacion=pr.id_presentacion
            WHERE pr.id_tienda=? AND pr.id_producto=? AND pr.estado='ACTIVA' ORDER BY pr.id_presentacion
            """, (rs, n) -> presentacion(rs.getObject(1, UUID.class), rs.getBigDecimal(2), rs.getBigDecimal(5), rs.getInt(3), rs.getInt(4)), tienda, id);
        return new Producto(id, categoria, nombre, descripcion, fotos, presentaciones);
    }
    private Presentacion presentacion(UUID id, BigDecimal precio, BigDecimal precioAnterior, int stock, int stockMinimo) {
        var opciones = jdbc.query("""
            SELECT v.nombre, vv.valor FROM presentacion_valor pv JOIN variante v ON v.id_variante=pv.id_variante
            JOIN valor_variante vv ON vv.id_valor=pv.id_valor AND vv.id_variante=pv.id_variante
            WHERE pv.id_presentacion=? ORDER BY v.nombre
            """, (rs, n) -> new Opcion(rs.getString(1), rs.getString(2)), id);
        String disponibilidad = disponibilidad(stock, stockMinimo);
        Integer restantes = "ULTIMAS_UNIDADES".equals(disponibilidad) ? stock : null;
        return new Presentacion(id, precio, precioAnterior, stock > 0, opciones, disponibilidad, restantes);
    }
}
