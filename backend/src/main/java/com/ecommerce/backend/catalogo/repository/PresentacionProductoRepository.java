
package com.ecommerce.backend.catalogo.repository;

import com.ecommerce.backend.catalogo.model.PresentacionProducto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PresentacionProductoRepository
        extends JpaRepository<PresentacionProducto, UUID> {

    List<PresentacionProducto> findByIdProducto(UUID idProducto);

    List<PresentacionProducto> findByIdProductoAndEstado(
            UUID idProducto,
            String estado
    );

    Optional<PresentacionProducto> findByIdPresentacionAndIdTienda(
            UUID idPresentacion,
            UUID idTienda
    );

    Optional<PresentacionProducto> findByIdTiendaAndSku(
            UUID idTienda,
            String sku
    );

    boolean existsByIdTiendaAndSku(
            UUID idTienda,
            String sku
    );
    boolean existsByIdProductoAndFirmaValores(
        UUID idProducto,
        String firmaValores
);

boolean existsByIdProductoAndFirmaValoresAndIdPresentacionNot(
        UUID idProducto,
        String firmaValores,
        UUID idPresentacion
);

Optional<PresentacionProducto> findByIdPresentacionAndIdProducto(
        UUID idPresentacion,
        UUID idProducto
);


}
