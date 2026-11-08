
package com.ecommerce.backend.catalogo.repository;

import com.ecommerce.backend.catalogo.model.ColeccionProducto;
import com.ecommerce.backend.catalogo.model.ColeccionProductoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ColeccionProductoRepository
        extends JpaRepository<ColeccionProducto, ColeccionProductoId> {

    List<ColeccionProducto> findById_IdColeccion(UUID idColeccion);

    List<ColeccionProducto> findById_IdProducto(UUID idProducto);

    boolean existsById_IdColeccionAndId_IdProducto(
            UUID idColeccion,
            UUID idProducto
    );

    void deleteById_IdColeccionAndId_IdProducto(
            UUID idColeccion,
            UUID idProducto
    );
}
