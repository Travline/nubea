package com.ecommerce.backend.cuentapago.repository;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CuentaPagoConsultaRepository {
    private final EntityManager entityManager;

    public Optional<DatosCuentaPago> consultar(UUID idTienda) {
        var filas = entityManager.createNativeQuery("""
                SELECT proveedor, estado, id_cuenta_externa, fecha_vinculacion, fecha_actualizacion
                FROM cuenta_pago_tienda WHERE id_tienda = :idTienda
                """).setParameter("idTienda", idTienda).getResultList();
        if (filas.isEmpty()) return Optional.empty();
        Object[] fila = (Object[]) filas.get(0);
        return Optional.of(new DatosCuentaPago((String) fila[0], (String) fila[1], (String) fila[2],
                convertirFecha(fila[3]), convertirFecha(fila[4])));
    }

    private static OffsetDateTime convertirFecha(Object valor) {
        return valor == null ? null : ((Instant) valor).atOffset(ZoneOffset.UTC);
    }

    public record DatosCuentaPago(String proveedor, String estado, String idCuentaExterna,
                                  OffsetDateTime fechaVinculacion, OffsetDateTime fechaActualizacion) {}
}
