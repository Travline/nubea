package com.ecommerce.backend;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Datos de compra para tests del Bloque 3: tienda ACTIVA con cuenta de pago ACTIVA, categoría, comprador con
 * dirección y relación con la tienda. Precio por defecto 11.80 (IGV 18 %: base 10.00 + IGV 1.80 por unidad).
 */
final class CompraFixture {
    final JdbcTemplate jdbc;
    final UUID dueno = UUID.randomUUID(), comprador = UUID.randomUUID(), tienda = UUID.randomUUID(),
            categoria = UUID.randomUUID(), direccion = UUID.randomUUID();
    final String slug;

    CompraFixture(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.slug = "t-" + tienda;
        for (UUID u : new UUID[]{dueno, comprador})
            jdbc.update("INSERT INTO usuario_plataforma(id_usuario,nombres,email) VALUES (?, 'Usuario', ?)", u, u + "@example.test");
        jdbc.update("INSERT INTO usuario_rol(id_usuario,id_rol) SELECT ?,id_rol FROM rol WHERE nombre='DUENO_TIENDA'", dueno);
        jdbc.update("INSERT INTO tienda(id_tienda,id_propietario,nombre,slug,estado) VALUES (?,?,'Tienda',?,'ACTIVA')", tienda, dueno, slug);
        jdbc.update("INSERT INTO cuenta_pago_tienda(id_tienda,estado,id_cuenta_externa,referencia_credencial,fecha_vinculacion) VALUES (?,'ACTIVA',?,'cifrado',now())",
                tienda, "cta-" + tienda);
        jdbc.update("INSERT INTO categoria(id_categoria,id_tienda,nombre,slug) VALUES (?,?,'Ropa','ropa')", categoria, tienda);
        jdbc.update("INSERT INTO tienda_cliente(id_usuario,id_tienda) VALUES (?,?)", comprador, tienda);
        jdbc.update("INSERT INTO direccion_cliente(id_direccion,id_usuario,direccion) VALUES (?,?,'Calle 123')", direccion, comprador);
    }

    UUID producto(String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO producto(id_producto,id_categoria,id_tienda,nombre,precio_base,estado) VALUES (?,?,?,?,11.80,?)",
                id, categoria, tienda, "Producto " + id.toString().substring(0, 6), estado);
        return id;
    }

    UUID version(UUID producto, String estado, int stock) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO presentacion_producto(id_presentacion,id_producto,id_tienda,sku,estado,firma_valores) VALUES (?,?,?,?,?,?)",
                id, producto, tienda, "SKU-" + id, estado, id.toString());
        jdbc.update("INSERT INTO inventario(id_presentacion,stock_actual) VALUES (?,?)", id, stock);
        return id;
    }

    /** Añade al carrito ACTIVO del comprador (lo crea si no existe). */
    UUID alCarrito(UUID version, int cantidad) {
        jdbc.update("INSERT INTO carrito(id_carrito,id_usuario,id_tienda) VALUES (?,?,?) ON CONFLICT (id_usuario,id_tienda) WHERE estado='ACTIVO' DO NOTHING",
                UUID.randomUUID(), comprador, tienda);
        UUID carrito = carritoActivo();
        jdbc.update("INSERT INTO carrito_item(id_carrito,id_presentacion,cantidad) VALUES (?,?,?) ON CONFLICT (id_carrito,id_presentacion) DO UPDATE SET cantidad=excluded.cantidad",
                carrito, version, cantidad);
        return carrito;
    }

    UUID carritoActivo() {
        var filas = jdbc.query("SELECT id_carrito FROM carrito WHERE id_usuario=? AND id_tienda=? AND estado='ACTIVO'",
                (r, n) -> r.getObject(1, UUID.class), comprador, tienda);
        return filas.isEmpty() ? null : filas.getFirst();
    }

    Integer cantidadEnCarrito(UUID version) {
        var filas = jdbc.query("""
                SELECT ci.cantidad FROM carrito_item ci JOIN carrito c ON c.id_carrito=ci.id_carrito
                WHERE c.id_usuario=? AND c.id_tienda=? AND c.estado='ACTIVO' AND ci.id_presentacion=?
                """, (r, n) -> r.getInt(1), comprador, tienda, version);
        return filas.isEmpty() ? null : filas.getFirst();
    }

    int stock(UUID version) {
        return jdbc.queryForObject("SELECT stock_actual FROM inventario WHERE id_presentacion=?", Integer.class, version);
    }

    static BigDecimal importe(String valor) {
        return new BigDecimal(valor);
    }
}
