
package com.ecommerce.backend.catalogo.repository;

import com.ecommerce.backend.catalogo.model.PresentacionValor;
import com.ecommerce.backend.catalogo.model.PresentacionValorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PresentacionValorRepository
        extends JpaRepository<PresentacionValor, PresentacionValorId> {

    List<PresentacionValor> findById_IdPresentacion(UUID idPresentacion);

    List<PresentacionValor> findById_IdValor(UUID idValor);

    Optional<PresentacionValor> findById_IdPresentacionAndId_IdValor(
            UUID idPresentacion,
            UUID idValor
    );

    boolean existsById_IdPresentacionAndId_IdValor(
            UUID idPresentacion,
            UUID idValor
    );

    void deleteById_IdPresentacion(UUID idPresentacion);
    
}
