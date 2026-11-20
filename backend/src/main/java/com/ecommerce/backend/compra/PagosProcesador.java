package com.ecommerce.backend.compra;

import com.ecommerce.backend.cuentapago.oauth.CredencialesVendedorService;
import com.ecommerce.backend.shared.exception.ReglaNegocioException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix="mercadopago.pagos",name="enabled",havingValue="true")
public class PagosProcesador {
    private final PagosStore store;
    private final CredencialesVendedorService credenciales;
    private final MercadoPagoOrders mercado;
    @Scheduled(fixedDelayString="${mercadopago.pagos.delay-ms:5000}")
    public void ejecutar() {
        for (var aviso:store.sinAsociar()) {
            if (!store.reclamarNotificacion(aviso.order())) continue;
            try {
                var token=credenciales.obtener(aviso.tienda());
                if (!aviso.cuenta().equals(token.cuenta())) continue;
                String ambiente=mercado.verificarCuenta(token.access(),aviso.cuenta());
                store.asociarRecuperada(aviso,ambiente,mercado.consultar(token.access(),aviso.order()));
            } catch (RuntimeException ex) { /* La cola conserva el aviso para el siguiente intento. */ }
        }
        store.despertarNotificaciones();
        for (UUID pedido:store.pendientes()) procesar(pedido);
    }
    public void procesar(UUID pedido) {
        UUID dueno=UUID.randomUUID();
        if (!store.reclamar(pedido,dueno)) return;
        String error=null,order=null;
        long version=0;
        UUID tienda=null;
        try {
            var t=store.leer(pedido); tienda=t.tienda(); order=t.order();
            if (order==null && !"PENDIENTE_PAGO".equals(t.estado())) return;
            var token=credenciales.obtener(t.tienda());
            if (!token.cuenta().equals(t.cuenta()) || !t.ambiente().equals(mercado.verificarCuenta(token.access(),t.cuenta())))
                throw new ReglaNegocioException("CUENTA_O_AMBIENTE_DIFERENTE");
            if (order==null) {
                if (!store.iniciarCreacion(pedido,dueno)) return;
                var creada=mercado.crear(token.access(),t.clave(),pedido,t.total(),store.items(pedido),URI.create(t.retorno()),store.tiposExcluidos(pedido));
                store.creada(pedido,dueno,creada);
                order=creada.id(); t=store.leer(pedido);
            }
            version=store.versionNotificacion(order);
            var orden=mercado.consultar(token.access(),order);
            store.aplicar(pedido,dueno,orden);
            t=store.leer(pedido);
            UUID reembolso=store.reembolsoPendiente(pedido);
            if (reembolso!=null && "processed".equals(orden.estado()) && "accredited".equals(orden.detalle())) {
                mercado.reembolsar(token.access(),order,reembolso);
                store.aplicar(pedido,dueno,mercado.consultar(token.access(),order));
            } else if ("PENDIENTE_PAGO".equals(t.estado()) && t.vence().isBefore(OffsetDateTime.now())) {
                mercado.cancelar(token.access(),order,t.cancelacion());
                store.aplicar(pedido,dueno,mercado.consultar(token.access(),order));
            }
        } catch (MercadoPagoOrders.TokenRechazado ex) {
            error="AUTORIZACION_PENDIENTE";
            if (tienda!=null) try { credenciales.obtener(tienda,true); } catch (RuntimeException ignored) { /* reintentar o reconectar */ }
        } catch (ReglaNegocioException ex) {
            error="REQUIERE_REVISION";
        } catch (RuntimeException ex) {
            error="PROVEEDOR_NO_CONFIRMADO";
        } finally {
            try { store.terminar(pedido,dueno,order,version,error); }
            catch (RuntimeException ex) {
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("No se pudo cerrar trabajo de pago {}; se recuperará por vencimiento",pedido);
            }
        }
    }
}
