
package com.ecommerce.backend.catalogo.repository;

import com.ecommerce.backend.catalogo.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    List<Categoria> findByIdTienda(UUID idTienda);

    List<Categoria> findByIdTiendaAndActivaTrue(UUID idTienda);

    Optional<Categoria> findByIdCategoriaAndIdTienda(
            UUID idCategoria,
            UUID idTienda
    );

    boolean existsByIdTiendaAndSlug(UUID idTienda, String slug);
}
