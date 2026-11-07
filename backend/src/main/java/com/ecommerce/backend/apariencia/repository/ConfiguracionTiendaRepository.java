package com.ecommerce.backend.apariencia.repository;

import com.ecommerce.backend.apariencia.model.ConfiguracionTienda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConfiguracionTiendaRepository extends JpaRepository<ConfiguracionTienda, UUID> {
}
