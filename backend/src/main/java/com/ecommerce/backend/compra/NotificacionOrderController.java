package com.ecommerce.backend.compra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** El contenido no es autoridad sobre el estado de pago: solo encola una consulta. */
@RestController
@ConditionalOnProperty(prefix = "mercadopago.notificaciones", name = "enabled", havingValue = "true")
public class NotificacionOrderController {
    private final JdbcTemplate jdbc;
    private final String secreto;

    public NotificacionOrderController(JdbcTemplate jdbc,
            @Value("${mercadopago.notificaciones.secret:}") String secreto) {
        if (secreto == null || secreto.isBlank()) throw new IllegalStateException("Falta la firma de notificaciones de Mercado Pago");
        this.jdbc = jdbc;
        this.secreto = secreto;
    }

    public ResponseEntity<Void> recibir(String id,String tipo,String firma,String requestId) {
        return recibir(id,tipo,firma,requestId,java.util.Map.of());
    }
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/api/publico/mercadopago/notificaciones")
    public ResponseEntity<Void> recibir(
            @RequestParam(name = "data.id", required = false) String id,
            @RequestParam(name = "type", required = false) String tipo,
            @RequestHeader(name = "x-signature", required = false) String firma,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @org.springframework.web.bind.annotation.RequestBody(required=false) java.util.Map<String,Object> cuerpo) {
        if (!FirmaWebhookMercadoPago.verificar(secreto, firma, requestId, id))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!"order".equals(tipo)) return ResponseEntity.badRequest().build();
        // La firma cubre el ID del query; los datos del cuerpo no se usan para autorizar cambios.
        jdbc.update("""
            INSERT INTO notificacion_order_mercadopago(id_order) VALUES (?)
            ON CONFLICT (id_order) DO UPDATE SET ultima_recepcion=now(),
                version=notificacion_order_mercadopago.version+1
            """, id);
        if (cuerpo!=null && cuerpo.get("user_id")!=null) {
            String receptor=String.valueOf(cuerpo.get("user_id"));
            if (receptor.matches("[0-9]{1,100}")) jdbc.update("UPDATE notificacion_order_mercadopago SET receptor_sugerido=? WHERE id_order=?",receptor,id);
        }
        return ResponseEntity.ok().build();
    }
}
