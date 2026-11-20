package com.ecommerce.backend.compra;

import com.ecommerce.backend.shared.exception.RecursoNoEncontradoException;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    public record Resultado(UUID idPedido, BigDecimal total) {}
    private record Direccion(String receptor, String telefono, String direccion, String referencia, String codigo,
                             String departamento, String provincia, String distrito) {}
    private record Impuesto(short id, BigDecimal tasa) {}
    private record Item(UUID id, String nombre, String opciones, int cantidad, BigDecimal precio, boolean valida) {}
    private record Importe(Item item, BigDecimal subtotal, BigDecimal impuesto, BigDecimal total) {}

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
        return resultado;
    }

    @Transactional
    public Resultado reservar(UUID usuario, String slug, UUID idDireccion) {
        return reservar(usuario, slug, idDireccion, null);
    }

    private Resultado reservar(UUID usuario, String slug, UUID idDireccion, BigDecimal totalEsperado) {
        jdbc.queryForObject("SELECT id_usuario FROM usuario_plataforma WHERE id_usuario=? FOR UPDATE",UUID.class,usuario);
        var tiendas=jdbc.query("SELECT id_tienda FROM tienda WHERE slug=? AND estado='ACTIVA' FOR UPDATE",(r,n)->r.getObject(1,UUID.class),slug);
        if (tiendas.isEmpty()) throw new RecursoNoEncontradoException("Tienda no disponible");
        UUID tienda=tiendas.getFirst();
        var cuentas=jdbc.query("SELECT estado FROM cuenta_pago_tienda WHERE id_tienda=? FOR UPDATE",(r,n)->r.getString(1),tienda);
        if (cuentas.isEmpty() || !"ACTIVA".equals(cuentas.getFirst())) throw new ReglaNegocioException("La tienda no tiene una cuenta de pago activa");
        var carritos=jdbc.query("SELECT id_carrito FROM carrito WHERE id_usuario=? AND id_tienda=? AND estado='ACTIVO' FOR UPDATE",(r,n)->r.getObject(1,UUID.class),usuario,tienda);
        if (carritos.isEmpty()) throw new ReglaNegocioException("El carrito está vacío");
        UUID carrito=carritos.getFirst();
        var direcciones=jdbc.query("""
            SELECT u.nombres,d.telefono_contacto,d.direccion,d.referencia,d.codigo_ubigeo,b.departamento,b.provincia,b.distrito
            FROM direccion_cliente d JOIN usuario_plataforma u ON u.id_usuario=d.id_usuario JOIN ubigeo b ON b.codigo_ubigeo=d.codigo_ubigeo
            WHERE d.id_direccion=? AND d.id_usuario=?
            """,(r,n)->new Direccion(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8)),idDireccion,usuario);
        if (direcciones.isEmpty()) throw new RecursoNoEncontradoException("Dirección no encontrada");
        var impuestos=jdbc.query("SELECT id_impuesto,tasa_porcentaje FROM configuracion_impuesto WHERE codigo='IGV_GENERAL' AND fecha_inicio<=CURRENT_DATE AND (fecha_fin IS NULL OR fecha_fin>CURRENT_DATE)",
                (r,n)->new Impuesto(r.getShort(1),r.getBigDecimal(2)));
        if (impuestos.size()!=1) throw new ReglaNegocioException("No hay una tasa de impuesto vigente");
        var impuesto=impuestos.getFirst();
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
        if (items.stream().anyMatch(i -> !i.valida())) throw new ReglaNegocioException("El carrito contiene productos no disponibles");
        BigDecimal factor=BigDecimal.ONE.add(impuesto.tasa().divide(BigDecimal.valueOf(100)));
        List<Importe> importes=items.stream().map(i->{
            BigDecimal total=i.precio().multiply(BigDecimal.valueOf(i.cantidad())).setScale(2,RoundingMode.UNNECESSARY);
            BigDecimal subtotal=total.divide(factor,2,RoundingMode.HALF_UP);
            return new Importe(i,subtotal,total.subtract(subtotal),total);
        }).toList();
        BigDecimal total=importes.stream().map(Importe::total).reduce(BigDecimal.ZERO,BigDecimal::add);
        if (total.signum()<=0) throw new ReglaNegocioException("No se puede finalizar un pedido de total cero");
        if (totalEsperado != null && total.compareTo(totalEsperado) != 0)
            throw new ReglaNegocioException("El total del carrito cambió; revisa los importes antes de continuar");
        BigDecimal subtotal=importes.stream().map(Importe::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal montoImpuesto=importes.stream().map(Importe::impuesto).reduce(BigDecimal.ZERO,BigDecimal::add);
        for (Item item:items) if (jdbc.update("UPDATE inventario SET stock_actual=stock_actual-?,fecha_actualizacion=now() WHERE id_presentacion=? AND stock_actual>=?",item.cantidad(),item.id(),item.cantidad())!=1)
            throw new ReglaNegocioException("No hay suficiente stock para " + item.nombre());
        UUID pedido=UUID.randomUUID(); Direccion d=direcciones.getFirst();
        jdbc.update("""
            INSERT INTO pedido(id_pedido,id_usuario,id_tienda,id_carrito,id_impuesto,fecha_expiracion_reserva,subtotal,tasa_impuesto,monto_impuesto,total,
                nombre_receptor,telefono_receptor,direccion_entrega,referencia_entrega,ubigeo_entrega,departamento_entrega,provincia_entrega,distrito_entrega)
            VALUES (?,?,?,?,?,now()+interval '30 minutes',?,?,?,?,?,?,?,?,?,?,?,?)
            """,pedido,usuario,tienda,carrito,impuesto.id(),subtotal,impuesto.tasa(),montoImpuesto,total,d.receptor(),d.telefono(),d.direccion(),d.referencia(),d.codigo(),d.departamento(),d.provincia(),d.distrito());
        for (Importe i:importes) jdbc.update("""
            INSERT INTO pedido_item(id_pedido_item,id_pedido,id_presentacion,nombre_producto,descripcion_presentacion,cantidad,precio_unitario,subtotal,monto_impuesto,total_linea)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """,UUID.randomUUID(),pedido,i.item().id(),i.item().nombre(),i.item().opciones(),i.item().cantidad(),i.item().precio(),i.subtotal(),i.impuesto(),i.total());
        jdbc.update("UPDATE carrito SET estado='CERRADO',fecha_actualizacion=now() WHERE id_carrito=?",carrito);
        return new Resultado(pedido,total);
    }
}
