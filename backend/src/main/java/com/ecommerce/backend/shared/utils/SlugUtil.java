package com.ecommerce.backend.shared.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {

    /** Mismo formato que exigen los CHECK de slug en la BD. */
    private static final Pattern FORMATO = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    private SlugUtil() {
        // Constructor privado para evitar instanciación
    }

    public static String generar(String texto) {
        return generar(texto, "tienda", 140);
    }

    /**
     * Genera un slug en minúsculas, sin tildes y con guiones.
     * Deja espacio para un sufijo de unicidad ("-2", "-3"...) respecto a {@code largoMaximo}.
     */
    public static String generar(String texto, String porDefecto, int largoMaximo) {
        String s = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")          // Eliminar acentos
                .toLowerCase(Locale.ROOT)          // Convertir a minúsculas
                .replaceAll("[^a-z0-9]+", "-")     // Reemplazar caracteres no alfanuméricos por guiones
                .replaceAll("^-|-$", "");          // Eliminar guiones al inicio y al final
        if (s.length() > largoMaximo) s = s.substring(0, largoMaximo).replaceAll("-+$", "");
        return s.isEmpty() ? porDefecto : s;
    }

    public static boolean esValido(String slug) {
        return slug != null && FORMATO.matcher(slug).matches();
    }
}
