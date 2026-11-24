package com.ecommerce.backend.shared.exception;

import static com.ecommerce.backend.shared.exception.MensajesUsuario.*;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;

/**
 * Traduce el nombre de una restricción de PostgreSQL al campo afectado y a un mensaje para el usuario.
 * Detección: primero {@link ConstraintViolationException#getConstraintName()} en la cadena de causas;
 * si no está (p. ej. JdbcTemplate), se busca el nombre dentro del texto del mensaje.
 */
public final class RestriccionesBd {

    public record Traduccion(String campo, String mensaje) {
    }

    public enum Tipo { UNICA, CHECK, OTRA }

    public record Violacion(String restriccion, Tipo tipo) {
    }

    private static final Map<String, Traduccion> TABLA = Map.ofEntries(
            Map.entry("tienda_slug_key", new Traduccion("slug", TIENDA_DIRECCION_DUPLICADA)),
            Map.entry("chk_tienda_slug", new Traduccion("slug", DIRECCION_FORMATO)),
            Map.entry("categoria_id_tienda_slug_key", new Traduccion("slug", CATEGORIA_DIRECCION_DUPLICADA)),
            Map.entry("categoria_slug_check", new Traduccion("slug", DIRECCION_FORMATO)),
            Map.entry("coleccion_id_tienda_slug_key", new Traduccion("slug", COLECCION_DIRECCION_DUPLICADA)),
            Map.entry("coleccion_slug_check", new Traduccion("slug", DIRECCION_FORMATO)),
            Map.entry("categoria_nombre_check", new Traduccion("nombre", NOMBRE_VACIO)),
            Map.entry("coleccion_nombre_check", new Traduccion("nombre", NOMBRE_VACIO)),
            Map.entry("producto_nombre_check", new Traduccion("nombre", NOMBRE_VACIO)),
            Map.entry("variante_nombre_check", new Traduccion("nombre", NOMBRE_VACIO)),
            Map.entry("uq_variante_nombre_ci", new Traduccion("nombre", OPCION_DUPLICADA)),
            Map.entry("uq_variante_tienda_nombre", new Traduccion("nombre", OPCION_DUPLICADA)),
            Map.entry("uq_valor_variante_ci", new Traduccion("valor", VALOR_DUPLICADO)),
            Map.entry("uq_valor_variante", new Traduccion("valor", VALOR_DUPLICADO)),
            Map.entry("valor_variante_valor_check", new Traduccion("valor", VALOR_VACIO)),
            Map.entry("uq_sku_tienda", new Traduccion("sku", CODIGO_DUPLICADO)),
            Map.entry("presentacion_producto_sku_check", new Traduccion("sku", CODIGO_VACIO)),
            Map.entry("uq_presentacion_combinacion", new Traduccion("idsValores", COMBINACION_REPETIDA)),
            Map.entry("uq_pv_una_por_variante", new Traduccion("idsValores", UN_VALOR_POR_OPCION)),
            Map.entry("producto_precio_base_check", new Traduccion("precioBase", PRECIO_NEGATIVO)),
            Map.entry("presentacion_producto_precio_override_check", new Traduccion("precioOverride", PRECIO_NEGATIVO)),
            Map.entry("chk_inventario_stock", new Traduccion("stockActual", UNIDADES_NEGATIVAS)),
            Map.entry("chk_inventario_minimo", new Traduccion("stockMinimo", UNIDADES_NEGATIVAS)),
            Map.entry("uq_imagen_principal_producto", new Traduccion("esPrincipal", FOTO_PRINCIPAL_DUPLICADA)),
            Map.entry("uq_banner_tienda_orden", new Traduccion("orden", POSICION_BANNER_OCUPADA)),
            Map.entry("usuario_plataforma_email_key", new Traduccion("correoElectronico", EMAIL_DUPLICADO)),
            Map.entry("uq_usuario_documento", new Traduccion("numeroDocumento", DOCUMENTO_DUPLICADO)));

    private RestriccionesBd() {
    }

    public static Optional<Traduccion> traducir(String restriccion) {
        return restriccion == null ? Optional.empty()
                : Optional.ofNullable(TABLA.get(restriccion.toLowerCase(Locale.ROOT)));
    }

    /** Identifica la restricción violada y si es UNIQUE o CHECK. */
    public static Violacion detectar(Throwable error) {
        String nombre = null;
        Tipo tipo = Tipo.OTRA;
        StringBuilder textos = new StringBuilder();
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (nombre == null && t instanceof ConstraintViolationException cve && cve.getConstraintName() != null) {
                nombre = cve.getConstraintName();
                if (cve.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) tipo = Tipo.UNICA;
                else if (cve.getKind() == ConstraintViolationException.ConstraintKind.CHECK) tipo = Tipo.CHECK;
            }
            if (t instanceof SQLException sql && tipo == Tipo.OTRA) {
                if ("23505".equals(sql.getSQLState())) tipo = Tipo.UNICA;
                else if ("23514".equals(sql.getSQLState())) tipo = Tipo.CHECK;
            }
            if (t.getMessage() != null) textos.append(t.getMessage()).append('\n');
        }
        if (nombre == null || !TABLA.containsKey(nombre.toLowerCase(Locale.ROOT))) {
            String texto = textos.toString();
            // El nombre más largo primero: "uq_valor_variante_ci" antes que "uq_valor_variante".
            nombre = TABLA.keySet().stream()
                    .sorted((a, b) -> b.length() - a.length())
                    .filter(clave -> contieneNombre(texto, clave))
                    .findFirst().orElse(nombre);
        }
        return new Violacion(nombre, tipo);
    }

    private static boolean contieneNombre(String texto, String nombre) {
        int i = texto.indexOf(nombre);
        while (i >= 0) {
            int fin = i + nombre.length();
            boolean inicioOk = i == 0 || !esParteDeNombre(texto.charAt(i - 1));
            boolean finOk = fin >= texto.length() || !esParteDeNombre(texto.charAt(fin));
            if (inicioOk && finOk) return true;
            i = texto.indexOf(nombre, i + 1);
        }
        return false;
    }

    private static boolean esParteDeNombre(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
