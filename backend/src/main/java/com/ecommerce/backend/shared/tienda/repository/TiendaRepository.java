package com.ecommerce.backend.shared.tienda.repository;

import com.ecommerce.backend.shared.tienda.model.Tienda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TiendaRepository extends JpaRepository<Tienda, UUID> {
}
