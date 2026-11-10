package com.ecommerce.backend.shared.utils;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtil {
    private SlugUtil() {
        // Constructor privado para evitar instanciación
    }
    public static String generar(String texto){
        String s = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "") // Eliminar acentos
                .toLowerCase(Locale.ROOT) // Convertir a minúsculas
                .replaceAll("[^a-z0-9]+", "-") // Reemplazar caracteres no alfanuméricos por guiones
                .replaceAll("^-|-$", ""); // Eliminar guiones al inicio y al final
        if (s.length() > 140) s = s.substring(0, 140).replaceAll("-$", "");
        
                return s.isEmpty() ? "tienda" : s; // Retornar "tienda" si el slug está vacío
    }
}
