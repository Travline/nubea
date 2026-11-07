package com.ecommerce.backend.plantilla.repository;

import com.ecommerce.backend.plantilla.model.Plantilla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlantillaRepository extends JpaRepository<Plantilla, Short> {

    Optional<Plantilla> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);
}
