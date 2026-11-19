
package com.ecommerce.backend.catalogo.repository;

import com.ecommerce.backend.catalogo.model.ImagenProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImagenProductoRepository
        extends JpaRepository<ImagenProducto, UUID> {

    List<ImagenProducto> findByIdProductoOrderByOrdenAsc(
            UUID idProducto
    );

    Optional<ImagenProducto> findByIdImagenAndIdProducto(
            UUID idImagen,
            UUID idProducto
    );

    Optional<ImagenProducto> findByIdProductoAndEsPrincipalTrue(
            UUID idProducto
    );

    boolean existsByStorageKey(String storageKey);
}
