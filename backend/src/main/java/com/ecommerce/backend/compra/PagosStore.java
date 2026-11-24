package com.ecommerce.backend.compra;

import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PagosStore {
    private final JdbcTemplate jdbc;
    private final CheckoutService checkout;
    private final PagosConfiguracion config;
    private final ReservasPedido reservas;
    public record Trabajo(UUID pedido, UUID tienda, UUID clave, String order, String cuenta, String ambiente,
                          String retorno, BigDecimal total, OffsetDateTime vence, UUID cancelacion, String estado) {}
    /**
     * {@code numero}: número corto del pedido en la tienda. {@code productosApartados}: solo en la respuesta del
     * checkout, líneas no disponibles que quedaron en un carrito nuevo (null en las consultas posteriores).
     */
    public record Estado(UUID idPedido, String estado, String estadoPago, String checkoutUrl, String ambiente,
                         String incidencia, String referencia, OffsetDateTime vencimiento, Integer numero,
                         Integer productosApartados) {
        public Estado conApartados(int apartados) {
            return new Estado(idPedido, estado, estadoPago, checkoutUrl, ambiente, incidencia, referencia, vencimiento, numero, apartados);
        }
    }

    @Transactional
    public UUID preparar(UUID usuario,String slug,UUID direccion,String clave,BigDecimal total,String cuenta,String ambiente) {
        return preparar(usuario,slug,direccion,clave,total,cuenta,ambiente,List.of("ticket","bank_transfer","atm","digital_currency","account_money","prepaid_card"));
    }
    public record Preparado(UUID pedido, int productosApartados) {}
    @Transactional
    public UUID preparar(UUID usuario,String slug,UUID direccion,String clave,BigDecimal total,String cuenta,String ambiente,List<String> tipos) {
        return prepararCompra(usuario,slug,direccion,clave,total,cuenta,ambiente,tipos).pedido();
    }
    @Transactional
    public Preparado prepararCompra(UUID usuario,String slug,UUID direccion,String clave,BigDecimal total,String cuenta,String ambiente,List<String> tipos) {
        var resultado=checkout.reservarIdempotente(usuario,slug,direccion,clave,total);
        UUID pedido=resultado.idPedido();
        Trabajo t=leer(pedido);
        if (t.cuenta()!=null) return new Preparado(pedido,resultado.productosApartados());
        var actuales=jdbc.query("SELECT id_cuenta_externa FROM cuenta_pago_tienda WHERE id_tienda=? AND estado='ACTIVA' FOR UPDATE",(r,n)->r.getString(1),t.tienda());
        if (actuales.size()!=1 || !cuenta.equals(actuales.getFirst()) || !ambiente.equals(config.ambiente(t.tienda())))
            throw new ReglaNegocioException("La cuenta de pago cambió; vuelve a intentar");
        jdbc.update("UPDATE cuenta_pago_tienda SET ambiente_pago=? WHERE id_tienda=?",ambiente,t.tienda());
        jdbc.update("UPDATE pedido SET ambiente_pago=? WHERE id_pedido=?",ambiente,pedido);
        jdbc.update("UPDATE intento_order_mercadopago SET ambiente_pago=?,cuenta_receptora=?,retorno_url=? WHERE id_pedido=?",ambiente,cuenta,config.retorno(slug,pedido),pedido);
        jdbc.update("UPDATE intento_order_mercadopago SET tipos_excluidos=? WHERE id_pedido=?",String.join(",",tipos),pedido);
        jdbc.update("INSERT INTO pago(id_pedido,clave_idempotencia,monto) VALUES (?,?,?)",pedido,"pedido:"+pedido,resultado.total());
        return new Preparado(pedido,resultado.productosApartados());
    }
    public Trabajo leer(UUID pedido) {
        return jdbc.queryForObject("""
            SELECT p.id_pedido,p.id_tienda,i.clave_proveedor,i.id_order,i.cuenta_receptora,i.ambiente_pago,i.retorno_url,
              p.total,p.fecha_expiracion_reserva,i.clave_cancelacion,p.estado
            FROM pedido p JOIN intento_order_mercadopago i ON i.id_pedido=p.id_pedido WHERE p.id_pedido=?
            """,(r,n)->new Trabajo(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getBigDecimal(8),r.getObject(9,OffsetDateTime.class),r.getObject(10,UUID.class),r.getString(11)),pedido);
    }
    public List<MercadoPagoOrders.Item> items(UUID pedido) {
        return jdbc.query("SELECT nombre_producto,cantidad,precio_unitario FROM pedido_item WHERE id_pedido=? ORDER BY id_pedido_item",
                (r,n)->new MercadoPagoOrders.Item(r.getString(1),r.getInt(2),r.getBigDecimal(3)),pedido);
    }
    public List<String> tiposExcluidos(UUID pedido) {
        String tipos=jdbc.queryForObject("SELECT tipos_excluidos FROM intento_order_mercadopago WHERE id_pedido=?",String.class,pedido);
        if (tipos==null || tipos.isBlank()) throw new ReglaNegocioException("Falta la configuración congelada de medios de pago");
        return List.of(tipos.split(","));
    }
    public Estado estado(UUID pedido) {
        return jdbc.queryForObject("""
            SELECT p.id_pedido,p.estado,pa.estado,
              CASE WHEN p.estado='PENDIENTE_PAGO' AND p.fecha_expiracion_reserva>now() THEN i.checkout_url END,
              p.ambiente_pago,i.ultimo_error,i.id_order,p.fecha_expiracion_reserva,p.numero
            FROM pedido p JOIN intento_order_mercadopago i ON i.id_pedido=p.id_pedido
            LEFT JOIN pago pa ON pa.clave_idempotencia='pedido:'||p.id_pedido::text WHERE p.id_pedido=?
            """,(r,n)->new Estado(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getObject(8,OffsetDateTime.class),r.getInt(9),null),pedido);
    }
    public List<UUID> pendientes() {
        return jdbc.query("""
            SELECT i.id_pedido FROM intento_order_mercadopago i
            WHERE cuenta_receptora IS NOT NULL AND proximo_intento<=now() AND (tarea_hasta IS NULL OR tarea_hasta<now())
            ORDER BY proximo_intento LIMIT 10
            """,(r,n)->r.getObject(1,UUID.class));
    }
    public boolean reclamar(UUID pedido,UUID dueno) {
        return jdbc.update("""
            UPDATE intento_order_mercadopago SET tarea_dueno=?,tarea_hasta=now()+interval '2 minutes'
            WHERE id_pedido=? AND cuenta_receptora IS NOT NULL AND (tarea_hasta IS NULL OR tarea_hasta<now())
            """,dueno,pedido)==1;
    }
    private void bloquear(UUID pedido,UUID dueno) {
        var filas=jdbc.query("SELECT id_pedido FROM intento_order_mercadopago WHERE id_pedido=? AND tarea_dueno=? AND tarea_hasta>now() FOR UPDATE",(r,n)->r.getObject(1,UUID.class),pedido,dueno);
        if (filas.isEmpty()) throw new ReglaNegocioException("El trabajo de pago fue recuperado por otro proceso");
        jdbc.queryForObject("SELECT id_pedido FROM pedido WHERE id_pedido=? FOR UPDATE",UUID.class,pedido);
    }
    @Transactional
    public boolean iniciarCreacion(UUID pedido,UUID dueno) {
        bloquear(pedido,dueno);
        var t=leer(pedido);
        String estado=jdbc.queryForObject("SELECT estado FROM intento_order_mercadopago WHERE id_pedido=?",String.class,pedido);
        if ("PREPARADO".equals(estado) && t.vence().isBefore(OffsetDateTime.now())) {
            cancelarLocal(pedido, true); return false;
        }
        // No reenviar creaciones antiguas fuera de la ventana de compra: la retención
        // de idempotencia del proveedor no debe asumirse indefinida.
        if ("CREACION_INCIERTA".equals(estado) && t.vence().isBefore(OffsetDateTime.now()))
            throw new ReglaNegocioException("CREACION_INCIERTA_VENCIDA");
        jdbc.update("UPDATE intento_order_mercadopago SET estado='CREACION_INCIERTA' WHERE id_pedido=?",pedido);
        return true;
    }
    @Transactional
    public void creada(UUID pedido,UUID dueno,MercadoPagoOrders.OrdenCreada orden) {
        bloquear(pedido,dueno);
        jdbc.update("UPDATE intento_order_mercadopago SET id_order=?,checkout_url=?,estado='CREADA',fecha_actualizacion=now() WHERE id_pedido=?",orden.id(),orden.checkoutUrl().toString(),pedido);
        jdbc.update("UPDATE pago SET referencia_externa=? WHERE clave_idempotencia=?",orden.id(),"pedido:"+pedido);
    }
    @Transactional
    public void aplicar(UUID pedido,UUID dueno,MercadoPagoOrders.OrdenConsultada orden) {
        bloquear(pedido,dueno);
        Trabajo t=leer(pedido);
        if (!java.util.Objects.equals(t.order(),orden.id()) || !pedido.toString().equals(orden.referencia())
                || !t.cuenta().equals(orden.vendedor()) || !"PEN".equals(orden.moneda()) || t.total().compareTo(orden.total())!=0)
            throw new ReglaNegocioException("La orden no coincide con el pedido");
        jdbc.update("UPDATE intento_order_mercadopago SET estado_proveedor=?,fecha_actualizacion=now() WHERE id_pedido=?",orden.estado()+":"+orden.detalle(),pedido);
        if ("processed".equals(orden.estado()) && "accredited".equals(orden.detalle())) {
            if (t.total().compareTo(orden.totalPagado())!=0) throw new ReglaNegocioException("Importe acreditado diferente");
            if ("PENDIENTE_PAGO".equals(t.estado())) {
                jdbc.update("UPDATE pago SET estado='APROBADO',fecha_confirmacion=now() WHERE clave_idempotencia=? AND estado='PENDIENTE'","pedido:"+pedido);
                jdbc.update("UPDATE pedido SET estado='PAGADO',estado_reserva='CONSUMIDA',fecha_pago=now() WHERE id_pedido=?",pedido);
                jdbc.update("UPDATE intento_order_mercadopago SET estado='FINALIZADA' WHERE id_pedido=?",pedido);
            } else if ("CANCELADO".equals(t.estado())) {
                jdbc.update("INSERT INTO reembolso_order(id_pedido,clave) VALUES (?,?) ON CONFLICT DO NOTHING",pedido,UUID.randomUUID());
                marcarReembolso(pedido);
            }
        } else if (("canceled".equals(orden.estado()) || "failed".equals(orden.estado())) && "PENDIENTE_PAGO".equals(t.estado())) {
            // "canceled" con la reserva vencida es la cancelación que pide el procesador al vencer: los productos
            // vuelven al carrito. Un pago rechazado ("failed") no los devuelve.
            cancelarLocal(pedido, "canceled".equals(orden.estado()) && t.vence().isBefore(OffsetDateTime.now()));
            if ("failed".equals(orden.estado())) jdbc.update("UPDATE pago SET estado='RECHAZADO' WHERE clave_idempotencia=? AND estado='CANCELADO'","pedido:"+pedido);
        } else if ("refunded".equals(orden.estado()) && "refunded".equals(orden.detalle())) {
            // Si el webhook de aprobación se perdió, cerrar la reserva sin entregar productos.
            if ("PENDIENTE_PAGO".equals(t.estado())) cancelarLocal(pedido, false);
            jdbc.update("UPDATE pago SET estado='REEMBOLSADO',fecha_confirmacion=COALESCE(fecha_confirmacion,now()) WHERE clave_idempotencia=?","pedido:"+pedido);
            jdbc.update("INSERT INTO reembolso_order(id_pedido,clave) VALUES (?,?) ON CONFLICT DO NOTHING",pedido,UUID.randomUUID());
            var reponer=jdbc.query("SELECT reponer_stock AND NOT stock_repuesto FROM reembolso_order WHERE id_pedido=? FOR UPDATE",(r,n)->r.getBoolean(1),pedido).getFirst();
            if (reponer && "PAGADO".equals(t.estado())) {
                jdbc.update("""
                    UPDATE inventario i SET stock_actual=i.stock_actual+pi.cantidad,fecha_actualizacion=now()
                    FROM pedido_item pi WHERE pi.id_pedido=? AND i.id_presentacion=pi.id_presentacion
                    """,pedido);
                jdbc.update("UPDATE reembolso_order SET stock_repuesto=true WHERE id_pedido=?",pedido);
            }
            jdbc.update("UPDATE reembolso_order SET estado='CONFIRMADO',fecha_confirmacion=COALESCE(fecha_confirmacion,now()) WHERE id_pedido=?",pedido);
            jdbc.update("UPDATE intento_order_mercadopago SET estado='REEMBOLSADA' WHERE id_pedido=?",pedido);
        } else if ("partially_refunded".equals(orden.detalle())) {
            throw new ReglaNegocioException("REEMBOLSO_PARCIAL_EXTERNO");
        }
    }
    private void cancelarLocal(UUID pedido, boolean porVencimiento) {
        reservas.cancelar(pedido, porVencimiento);
    }
    private void marcarReembolso(UUID pedido) {
        jdbc.update("UPDATE pago SET estado='REEMBOLSO_PENDIENTE',fecha_confirmacion=COALESCE(fecha_confirmacion,now()) WHERE clave_idempotencia=? AND estado<>'REEMBOLSADO'","pedido:"+pedido);
        jdbc.update("UPDATE intento_order_mercadopago SET estado='REEMBOLSO_PENDIENTE',proximo_intento=now() WHERE id_pedido=? AND estado<>'REEMBOLSADA'",pedido);
    }
    @Transactional
    public void solicitarReembolso(UUID tienda,UUID pedido,UUID usuario,UUID clave,boolean reponer) {
        // Serializar con desconexión (tienda -> cuenta); después usar el orden del procesador.
        jdbc.queryForObject("SELECT id_tienda FROM tienda WHERE id_tienda=? FOR UPDATE",UUID.class,tienda);
        var cuenta=jdbc.query("SELECT id_cuenta_externa FROM cuenta_pago_tienda WHERE id_tienda=? AND estado='ACTIVA' FOR UPDATE",(r,n)->r.getString(1),tienda);
        if (cuenta.isEmpty()) throw new ReglaNegocioException("Reconecta la cuenta original para solicitar la devolución");
        var filas=jdbc.query("""
            SELECT i.id_pedido FROM intento_order_mercadopago i JOIN pedido p ON p.id_pedido=i.id_pedido
            WHERE p.id_pedido=? AND p.id_tienda=? AND i.cuenta_receptora IS NOT NULL FOR UPDATE OF i
            """,(r,n)->r.getObject(1,UUID.class),pedido,tienda);
        if (filas.isEmpty()) throw new ReglaNegocioException("Pedido no disponible para reembolso");
        jdbc.queryForObject("SELECT id_pedido FROM pedido WHERE id_pedido=? FOR UPDATE",UUID.class,pedido);
        var existentes=jdbc.query("SELECT clave,reponer_stock FROM reembolso_order WHERE id_pedido=?",(r,n)->new Object[]{r.getObject(1,UUID.class),r.getBoolean(2)},pedido);
        if (!existentes.isEmpty()) {
            var e=existentes.getFirst();
            if (!clave.equals(e[0]) || !Boolean.valueOf(reponer).equals(e[1])) throw new ReglaNegocioException("Este pedido ya tiene un reembolso solicitado");
            return;
        }
        if (!cuenta.getFirst().equals(leer(pedido).cuenta()))
            throw new ReglaNegocioException("Reconecta la cuenta original del cobro para devolverlo");
        String proveedor=jdbc.queryForObject("SELECT estado_proveedor FROM intento_order_mercadopago WHERE id_pedido=?",String.class,pedido);
        if (!"processed:accredited".equals(proveedor)) throw new ReglaNegocioException("Consulta la operación en Mercado Pago antes de solicitar su devolución");
        Integer aprobados=jdbc.queryForObject("SELECT count(*) FROM pago WHERE clave_idempotencia=? AND estado='APROBADO'",Integer.class,"pedido:"+pedido);
        if (aprobados!=1) throw new ReglaNegocioException("El pedido no tiene un pago aprobado para devolver");
        jdbc.update("INSERT INTO reembolso_order(id_pedido,clave,solicitado_por,reponer_stock) VALUES (?,?,?,?)",pedido,clave,usuario,reponer);
        marcarReembolso(pedido);
    }
    public UUID reembolsoPendiente(UUID pedido) {
        var filas=jdbc.query("SELECT clave FROM reembolso_order WHERE id_pedido=? AND estado='PENDIENTE'",(r,n)->r.getObject(1,UUID.class),pedido);
        return filas.isEmpty()?null:filas.getFirst();
    }
    public long versionNotificacion(String id) {
        var filas=jdbc.query("SELECT version FROM notificacion_order_mercadopago WHERE id_order=?",(r,n)->r.getLong(1),id);
        return filas.isEmpty()?0:filas.getFirst();
    }
    @Transactional
    public void terminar(UUID pedido,UUID dueno,String order,long version,String error) {
        bloquear(pedido,dueno);
        if (error==null && order!=null) jdbc.update("UPDATE notificacion_order_mercadopago SET version_procesada=GREATEST(version_procesada,?) WHERE id_order=?",version,order);
        var t=leer(pedido);
        int intentos=jdbc.queryForObject("SELECT intentos FROM intento_order_mercadopago WHERE id_pedido=?",Integer.class,pedido);
        long demora=error==null ? ("PENDIENTE_PAGO".equals(t.estado()) || reembolsoPendiente(pedido)!=null ? 30:21600)
                : Math.min(3600,15L*(1L<<Math.min(intentos,8)))+java.util.concurrent.ThreadLocalRandom.current().nextInt(10);
        jdbc.update("""
            UPDATE intento_order_mercadopago SET tarea_dueno=NULL,tarea_hasta=NULL,ultimo_error=?,
              intentos=?,proximo_intento=now()+(? * interval '1 second') WHERE id_pedido=?
            """,error,error==null?0:Math.min(intentos+1,100),demora,pedido);
    }
    public record SinAsociar(String order,UUID tienda,String cuenta) {}
    public List<SinAsociar> sinAsociar() {
        return jdbc.query("""
            SELECT n.id_order,c.id_tienda,c.id_cuenta_externa FROM notificacion_order_mercadopago n
            JOIN cuenta_pago_tienda c ON c.id_cuenta_externa=n.receptor_sugerido AND c.estado='ACTIVA'
            WHERE n.version>n.version_procesada AND n.proxima_consulta<=now()
              AND NOT EXISTS (SELECT 1 FROM intento_order_mercadopago i WHERE i.id_order=n.id_order)
              AND EXISTS (SELECT 1 FROM intento_order_mercadopago i JOIN pedido p ON p.id_pedido=i.id_pedido
                  WHERE p.id_tienda=c.id_tienda AND i.cuenta_receptora=c.id_cuenta_externa AND i.id_order IS NULL)
            ORDER BY n.proxima_consulta LIMIT 5
            """,(r,n)->new SinAsociar(r.getString(1),r.getObject(2,UUID.class),r.getString(3)));
    }
    public boolean reclamarNotificacion(String order) {
        return jdbc.update("UPDATE notificacion_order_mercadopago SET proxima_consulta=now()+interval '5 minutes' WHERE id_order=? AND proxima_consulta<=now()",order)==1;
    }
    @Transactional
    public UUID asociarRecuperada(SinAsociar aviso,String ambiente,MercadoPagoOrders.OrdenConsultada orden) {
        UUID pedido;
        try { pedido=UUID.fromString(orden.referencia()); } catch (RuntimeException ex) { return null; }
        var filas=jdbc.query("""
            SELECT i.id_pedido FROM intento_order_mercadopago i JOIN pedido p ON p.id_pedido=i.id_pedido
            WHERE i.id_pedido=? AND p.id_tienda=? AND i.cuenta_receptora=? AND i.ambiente_pago=?
              AND i.id_order IS NULL AND (i.tarea_hasta IS NULL OR i.tarea_hasta<now()) FOR UPDATE OF i
            """,(r,n)->r.getObject(1,UUID.class),pedido,aviso.tienda(),aviso.cuenta(),ambiente);
        if (filas.isEmpty()) return null;
        var t=leer(pedido);
        if (!aviso.order().equals(orden.id()) || !aviso.cuenta().equals(orden.vendedor())
                || !"PEN".equals(orden.moneda()) || t.total().compareTo(orden.total())!=0) return null;
        jdbc.update("UPDATE intento_order_mercadopago SET id_order=?,estado='CREADA',proximo_intento=now(),ultimo_error=NULL WHERE id_pedido=?",orden.id(),pedido);
        jdbc.update("UPDATE pago SET referencia_externa=? WHERE clave_idempotencia=?",orden.id(),"pedido:"+pedido);
        return pedido;
    }
    public void despertarNotificaciones() {
        jdbc.update("""
            UPDATE intento_order_mercadopago i SET proximo_intento=now()
            FROM notificacion_order_mercadopago n WHERE n.id_order=i.id_order AND n.version>n.version_procesada
              AND i.proximo_intento>now() AND i.ultimo_error IS NULL
            """);
    }
}
