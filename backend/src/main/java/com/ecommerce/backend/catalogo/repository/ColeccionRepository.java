
package com.ecommerce.backend.catalogo.repository;

import com.ecommerce.backend.catalogo.model.Coleccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ColeccionRepository
        extends JpaRepository<Coleccion, UUID> {

    List<Coleccion> findByIdTienda(UUID idTienda);

    List<Coleccion> findByIdTiendaAndActivaTrue(UUID idTienda);

    Optional<Coleccion> findByIdColeccionAndIdTienda(
            UUID idColeccion,
            UUID idTienda
    );

    Optional<Coleccion> findByIdTiendaAndSlug(
            UUID idTienda,
            String slug
    );

    boolean existsByIdTiendaAndSlug(
            UUID idTienda,
            String slug
    );
}
