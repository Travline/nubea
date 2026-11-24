package com.ecommerce.backend.shared;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.backend.shared.exception.MensajesUsuario;
import com.ecommerce.backend.shared.utils.SlugUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class SlugUtilTest {

    @Test
    void generaMinusculasSinTildesConGuiones() {
        assertEquals("ropa-de-nino", SlugUtil.generar("  Ropa de Niño  ", "categoria", 120));
        assertEquals("cafe-te-mas", SlugUtil.generar("Café, Té & Más!", "categoria", 120));
        assertEquals("categoria", SlugUtil.generar("¡¡!!", "categoria", 120));
        assertEquals("categoria", SlugUtil.generar(null, "categoria", 120));
    }

    @Test
    void recortaAlLargoMaximoSinDejarGuionFinal() {
        String slug = SlugUtil.generar("abc def ghi", "x", 8);
        assertEquals("abc-def", slug);
        assertTrue(SlugUtil.esValido(slug));
    }

    @Test
    void elMetodoDeTiendaConservaSuComportamiento() {
        assertEquals("mi-tienda", SlugUtil.generar("Mi Tienda"));
        assertEquals("tienda", SlugUtil.generar("***"));
    }

    @Test
    void validaElMismoFormatoQueLaBd() {
        assertTrue(SlugUtil.esValido("ropa-de-nino-2"));
        assertFalse(SlugUtil.esValido("Ropa"));
        assertFalse(SlugUtil.esValido("ropa de nino"));
        assertFalse(SlugUtil.esValido("-ropa"));
        assertFalse(SlugUtil.esValido("ropa--nino"));
        assertFalse(SlugUtil.esValido("niño"));
        assertFalse(SlugUtil.esValido(null));
    }

    @Test
    void losMensajesDeUsuarioNoTienenJerga() throws Exception {
        for (Field campo : MensajesUsuario.class.getDeclaredFields()) {
            if (!Modifier.isStatic(campo.getModifiers()) || campo.getType() != String.class) continue;
            String texto = ((String) campo.get(null)).toLowerCase(Locale.ROOT);
            for (String prohibido : new String[] {"slug", "sku", "presentaci", "variante", "firma", "uq_", "chk_", "_key", "_check"}) {
                assertFalse(texto.contains(prohibido), campo.getName() + " contiene '" + prohibido + "': " + texto);
            }
        }
    }
}
