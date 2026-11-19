
package com.ecommerce.backend.catalogo.repository;

import com.ecommerce.backend.catalogo.model.Variante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VarianteRepository extends JpaRepository<Variante, UUID> {

    List<Variante> findByIdTienda(UUID idTienda);

    Optional<Variante> findByIdVarianteAndIdTienda(
            UUID idVariante,
            UUID idTienda
    );

    boolean existsByIdTiendaAndNombreIgnoreCase(
            UUID idTienda,
            String nombre
    );
}
