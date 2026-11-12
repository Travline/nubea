
package com.ecommerce.backend.catalogo.repository;

import com.ecommerce.backend.catalogo.model.PresentacionImagen;
import com.ecommerce.backend.catalogo.model.PresentacionImagenId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PresentacionImagenRepository
        extends JpaRepository<PresentacionImagen, PresentacionImagenId> {

    List<PresentacionImagen> findById_IdPresentacion(
            UUID idPresentacion
    );

    List<PresentacionImagen> findById_IdImagen(
            UUID idImagen
    );

    boolean existsById_IdPresentacionAndId_IdImagen(
            UUID idPresentacion,
            UUID idImagen
    );
}
