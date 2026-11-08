
package com.ecommerce.backend.catalogo.repository;

import com.ecommerce.backend.catalogo.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    List<Producto> findByIdTienda(UUID idTienda);

    List<Producto> findByIdTiendaAndEstado(
            UUID idTienda,
            String estado
    );

    List<Producto> findByIdTiendaAndIdCategoria(
            UUID idTienda,
            UUID idCategoria
    );

    Optional<Producto> findByIdProductoAndIdTienda(
            UUID idProducto,
            UUID idTienda
    );

    boolean existsByIdTiendaAndNombre(
            UUID idTienda,
            String nombre
    );
}
