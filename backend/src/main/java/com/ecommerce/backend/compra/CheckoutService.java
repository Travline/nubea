package com.ecommerce.backend.compra;

import com.ecommerce.backend.shared.tienda.TiendasPublicas;

import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import com.ecommerce.backend.shared.exception.MensajesUsuario;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Núcleo transaccional; no se expone por HTTP hasta completar la integración de pagos. */
@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final JdbcTemplate jdbc;
    /** {@code productosApartados}: líneas no disponibles que pasaron a un carrito ACTIVO nuevo (0 al repetir la clave). */
    public record Resultado(UUID idPedido, BigDecimal total, int productosApartados) {
        public Resultado(UUID idPedido, BigDecimal total) { this(idPedido, total, 0); }
    }
    private record Direccion(String receptor, String telefono, String direccion, String referencia, String codigo,
                             String departamento, String provincia, String distrito, String placeId) {}
    private record Item(UUID id, String nombre, String opciones, int cantidad, BigDecimal precio, boolean valida) {}
    private record Importe(Item item, CalculoImportes.Linea importe) {}

    /** La clave identifica la solicitud completa dentro del contexto de comprador y tienda. */
    @Transactional
    public Resultado reservarIdempotente(UUID usuario, String slug, UUID idDireccion, String clave, BigDecimal totalEsperado) {
        if (idDireccion == null || clave == null || !clave.matches("[A-Za-z0-9_-]{16,100}") || totalEsperado == null
                || totalEsperado.signum() <= 0 || totalEsperado.scale() > 2)
            throw new ReglaNegocioException("Datos de checkout inválidos");
        jdbc.queryForObject("SELECT id_usuario FROM usuario_plataforma WHERE id_usuario=? FOR UPDATE", UUID.class, usuario);
        var anteriores = jdbc.query("""
            SELECT c.id_direccion,c.total_esperado,p.id_pedido,p.total
            FROM checkout_idempotencia c JOIN pedido p ON p.id_pedido=c.id_pedido
            JOIN tienda t ON t.id_tienda=c.id_tienda
            WHERE c.id_usuario=? AND t.slug=? AND c.clave=?
            """, (r,n) -> new Object[]{r.getObject(1,UUID.class),r.getBigDecimal(2),r.getObject(3,UUID.class),r.getBigDecimal(4)}, usuario,slug,clave);
        if (!anteriores.isEmpty()) {
            var anterior = anteriores.getFirst();
            if (!idDireccion.equals(anterior[0]) || totalEsperado.compareTo((BigDecimal) anterior[1]) != 0)
                throw new ReglaNegocioException("La clave de idempotencia ya se usó con otros datos");
            return new Resultado((UUID) anterior[2], (BigDecimal) anterior[3]);
        }
        Resultado resultado = reservar(usuario, slug, idDireccion, totalEsperado);
        jdbc.update("""
            INSERT INTO checkout_idempotencia(id_usuario,id_tienda,clave,id_direccion,total_esperado,id_pedido)
            SELECT ?,id_tienda,?,?,?,? FROM pedido WHERE id_pedido=?
            """, usuario,clave,idDireccion,totalEsperado,resultado.idPedido(),resultado.idPedido());
        jdbc.update("INSERT INTO intento_order_mercadopago(id_intento,id_pedido,clave_proveedor) VALUES (?,?,?)",
                UUID.randomUUID(),resultado.idPedido(),UUID.randomUUID());
        return resultado;
    }

    @Transactional
    public Resultado reservar(UUID usuario, String slug, UUID idDireccion) {
        return reservar(usuario, slug, idDireccion, null);
    }

    private Resultado reservar(UUID usuario, String slug, UUID idDireccion, BigDecimal totalEsperado) {
        jdbc.queryForObject("SELECT id_usuario FROM usuario_plataforma WHERE id_usuario=? FOR UPDATE",UUID.class,usuario);
        var tiendas=jdbc.query("SELECT id_tienda FROM tienda WHERE slug=? AND estado='ACTIVA' FOR UPDATE",(r,n)->r.getObject(1,UUID.class),slug);
        if (tiendas.isEmpty()) throw TiendasPublicas.noActiva(jdbc, slug);
        UUID tienda=tiendas.getFirst();
        var cuentas=jdbc.query("SELECT estado FROM cuenta_pago_tienda WHERE id_tienda=? FOR UPDATE",(r,n)->r.getString(1),tienda);
        if (cuentas.isEmpty() || !"ACTIVA".equals(cuentas.getFirst())) throw new ReglaNegocioException("La tienda no tiene una cuenta de pago activa");
        var carritos=jdbc.query("SELECT id_carrito FROM carrito WHERE id_usuario=? AND id_tienda=? AND estado='ACTIVO' FOR UPDATE",(r,n)->r.getObject(1,UUID.class),usuario,tienda);
        if (carritos.isEmpty()) throw new ReglaNegocioException("El carrito está vacío");
        UUID carrito=carritos.getFirst();
        var direcciones=jdbc.query("""
            SELECT u.nombres,d.telefono_contacto,d.direccion,d.referencia,d.codigo_ubigeo,COALESCE(d.departamento,b.departamento),COALESCE(d.provincia,b.provincia),COALESCE(d.distrito,b.distrito),d.place_id
            FROM direccion_cliente d JOIN usuario_plataforma u ON u.id_usuario=d.id_usuario LEFT JOIN ubigeo b ON b.codigo_ubigeo=d.codigo_ubigeo
            WHERE d.id_direccion=? AND d.id_usuario=?
            """,(r,n)->new Direccion(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8),r.getString(9)),idDireccion,usuario);
        if (direcciones.isEmpty()) throw new RecursoNoEncontradoException("Dirección no encontrada");
        var impuesto=CalculoImportes.impuestoVigente(jdbc)
                .orElseThrow(() -> new ReglaNegocioException("No hay una tasa de impuesto vigente"));
        var items=jdbc.query("""
            SELECT pr.id_presentacion,p.nombre,
                COALESCE((SELECT string_agg(v.nombre || ': ' || vv.valor, ', ' ORDER BY v.nombre) FROM presentacion_valor pv
                  JOIN variante v ON v.id_variante=pv.id_variante JOIN valor_variante vv ON vv.id_valor=pv.id_valor WHERE pv.id_presentacion=pr.id_presentacion),''),
                ci.cantidad,COALESCE(pr.precio_override,p.precio_base),
                (pr.estado='ACTIVA' AND p.estado='PUBLICADO' AND c.activa=true)
            FROM carrito_item ci JOIN presentacion_producto pr ON pr.id_presentacion=ci.id_presentacion AND pr.id_tienda=?
            JOIN producto p ON p.id_producto=pr.id_producto AND p.id_tienda=pr.id_tienda
            JOIN categoria c ON c.id_categoria=p.id_categoria AND c.id_tienda=p.id_tienda
            WHERE ci.id_carrito=? ORDER BY pr.id_presentacion FOR UPDATE OF pr,p,c
            """,(r,n)->new Item(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getInt(4),r.getBigDecimal(5),r.getBoolean(6)),tienda,carrito);
        if (items.isEmpty()) throw new ReglaNegocioException("El carrito está vacío");

        // Bloquear el inventario en orden de id: la disponibilidad no puede cambiar hasta confirmar.
        java.util.Map<UUID,Integer> stock=new java.util.HashMap<>();
        jdbc.query("SELECT id_presentacion,stock_actual FROM inventario WHERE id_presentacion = ANY(?) ORDER BY id_presentacion FOR UPDATE",
                (java.sql.ResultSet r) -> { stock.put(r.getObject(1,UUID.class), r.getInt(2)); },
                (Object) items.stream().map(Item::id).toArray(UUID[]::new));
        // Checkout parcial (decisión del equipo): se compran solo las líneas disponibles; una línea con cantidad
        // mayor que el stock se aparta ENTERA. Las apartadas pasan a un carrito ACTIVO nuevo.
        List<Item> comprables=items.stream().filter(i -> i.valida() && stock.getOrDefault(i.id(),0) >= i.cantidad()).toList();
        List<Item> apartados=items.stream().filter(i -> !comprables.contains(i)).toList();
        if (comprables.isEmpty()) throw new ReglaNegocioException("items", MensajesUsuario.NINGUN_PRODUCTO_DISPONIBLE);

        List<Importe> importes=comprables.stream()
                .map(i->new Importe(i,CalculoImportes.linea(i.precio(),i.cantidad(),impuesto.tasa()))).toList();
        var totales=CalculoImportes.sumar(importes.stream().map(Importe::importe).toList());
        BigDecimal total=totales.total();
        if (total.signum()<=0) throw new ReglaNegocioException("No se puede finalizar un pedido de total cero");
        if (totalEsperado != null && total.compareTo(totalEsperado) != 0)
            throw new ReglaNegocioException("El total del carrito cambió; revisa los importes antes de continuar");
        for (Item item:comprables) if (jdbc.update("UPDATE inventario SET stock_actual=stock_actual-?,fecha_actualizacion=now() WHERE id_presentacion=? AND stock_actual>=?",item.cantidad(),item.id(),item.cantidad())!=1)
            throw new ReglaNegocioException("No hay suficiente stock para " + item.nombre());
        UUID pedido=UUID.randomUUID(); Direccion d=direcciones.getFirst();
        // El número corto (#1001…) lo asigna trg_pedido_numero con el contador de la tienda, ya bloqueada arriba.
        jdbc.update("""
            INSERT INTO pedido(id_pedido,id_usuario,id_tienda,id_carrito,id_impuesto,fecha_expiracion_reserva,subtotal,tasa_impuesto,monto_impuesto,total,
                nombre_receptor,telefono_receptor,direccion_entrega,referencia_entrega,ubigeo_entrega,departamento_entrega,provincia_entrega,distrito_entrega,place_id_entrega)
            VALUES (?,?,?,?,?,now()+interval '30 minutes',?,?,?,?,?,?,?,?,?,?,?,?,?)
            """,pedido,usuario,tienda,carrito,impuesto.id(),totales.subtotal(),impuesto.tasa(),totales.impuesto(),total,d.receptor(),d.telefono(),d.direccion(),d.referencia(),d.codigo(),d.departamento(),d.provincia(),d.distrito(),d.placeId());
        for (Importe i:importes) jdbc.update("""
            INSERT INTO pedido_item(id_pedido_item,id_pedido,id_presentacion,nombre_producto,descripcion_presentacion,cantidad,precio_unitario,subtotal,monto_impuesto,total_linea)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """,UUID.randomUUID(),pedido,i.item().id(),i.item().nombre(),i.item().opciones(),i.item().cantidad(),i.item().precio(),i.importe().subtotal(),i.importe().impuesto(),i.importe().total());
        // Cerrar primero el carrito de origen (uq_carrito_activo_usuario_tienda) y luego mover lo apartado a uno nuevo.
        jdbc.update("UPDATE carrito SET estado='CERRADO',fecha_actualizacion=now() WHERE id_carrito=?",carrito);
        if (!apartados.isEmpty()) {
            UUID nuevo=UUID.randomUUID();
            jdbc.update("INSERT INTO carrito(id_carrito,id_usuario,id_tienda) VALUES (?,?,?)",nuevo,usuario,tienda);
            jdbc.update("UPDATE carrito_item SET id_carrito=? WHERE id_carrito=? AND id_presentacion = ANY(?)",
                    nuevo,carrito,(Object) apartados.stream().map(Item::id).toArray(UUID[]::new));
        }
        return new Resultado(pedido,total,apartados.size());
    }
}
