package com.ecommerce.backend.vitrina;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Bloque 2, punto 4: umbral = stock mínimo de la versión si es mayor que 0; si no, 5. */
class DisponibilidadVitrinaTest {

    @Test
    void sinStockMinimoElUmbralEsCinco() {
        assertEquals("AGOTADO", VitrinaController.disponibilidad(0, 0));
        assertEquals("AGOTADO", VitrinaController.disponibilidad(-1, 0));
        assertEquals("ULTIMAS_UNIDADES", VitrinaController.disponibilidad(1, 0));
        assertEquals("ULTIMAS_UNIDADES", VitrinaController.disponibilidad(5, 0));
        assertEquals("DISPONIBLE", VitrinaController.disponibilidad(6, 0));
    }

    @Test
    void conStockMinimoSeUsaComoUmbral() {
        assertEquals("ULTIMAS_UNIDADES", VitrinaController.disponibilidad(10, 10));
        assertEquals("DISPONIBLE", VitrinaController.disponibilidad(11, 10));
        assertEquals("ULTIMAS_UNIDADES", VitrinaController.disponibilidad(2, 2));
        assertEquals("DISPONIBLE", VitrinaController.disponibilidad(3, 2));
    }
}
