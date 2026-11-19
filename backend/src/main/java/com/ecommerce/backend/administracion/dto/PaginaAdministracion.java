package com.ecommerce.backend.administracion.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaAdministracion<T>(List<T> contenido, int pagina, int totalPaginas, long totalElementos) {
    public static <T> PaginaAdministracion<T> desde(Page<T> pagina) {
        return new PaginaAdministracion<>(pagina.getContent(), pagina.getNumber(), pagina.getTotalPages(), pagina.getTotalElements());
    }
}
