package com.ecommerce.backend.compra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Cálculo de importes compartido por el carrito y el checkout (RN-M4-09/10), para que lo que se muestra
 * sea exactamente lo que se cobra. Los precios del catálogo incluyen IGV: por cada línea se extrae la base
 * con HALF_UP y el IGV es la diferencia; la cabecera es la suma de sus líneas.
 */
public final class CalculoImportes {
    private CalculoImportes() {
    }

    public record Impuesto(short id, BigDecimal tasa) {}
    public record Linea(BigDecimal subtotal, BigDecimal impuesto, BigDecimal total) {}
    public record Totales(BigDecimal subtotal, BigDecimal impuesto, BigDecimal total) {}

    /** Tasa de IGV vigente hoy, si hay exactamente una. */
    public static Optional<Impuesto> impuestoVigente(JdbcTemplate jdbc) {
        var impuestos = jdbc.query("""
                SELECT id_impuesto, tasa_porcentaje FROM configuracion_impuesto
                WHERE codigo='IGV_GENERAL' AND fecha_inicio<=CURRENT_DATE AND (fecha_fin IS NULL OR fecha_fin>CURRENT_DATE)
                """, (r, n) -> new Impuesto(r.getShort(1), r.getBigDecimal(2)));
        return impuestos.size() == 1 ? Optional.of(impuestos.getFirst()) : Optional.empty();
    }

    public static Linea linea(BigDecimal precioConIgv, int cantidad, BigDecimal tasa) {
        BigDecimal factor = BigDecimal.ONE.add(tasa.divide(BigDecimal.valueOf(100)));
        BigDecimal total = precioConIgv.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal subtotal = total.divide(factor, 2, RoundingMode.HALF_UP);
        return new Linea(subtotal, total.subtract(subtotal), total);
    }

    public static Totales sumar(List<Linea> lineas) {
        return new Totales(
                lineas.stream().map(Linea::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                lineas.stream().map(Linea::impuesto).reduce(BigDecimal.ZERO, BigDecimal::add),
                lineas.stream().map(Linea::total).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
