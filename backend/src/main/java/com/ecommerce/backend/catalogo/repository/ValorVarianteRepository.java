
package com.ecommerce.backend.catalogo.repository;

import com.ecommerce.backend.catalogo.model.ValorVariante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ValorVarianteRepository
        extends JpaRepository<ValorVariante, UUID> {

    List<ValorVariante> findByIdVariante(UUID idVariante);

    Optional<ValorVariante> findByIdValorAndIdVariante(
            UUID idValor,
            UUID idVariante
    );

    boolean existsByIdVarianteAndValorIgnoreCase(
            UUID idVariante,
            String valor
    );
}
