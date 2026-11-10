package com.ecommerce.backend.tienda.repository;
import com.ecommerce.backend.tienda.model.ConfiguracionTienda;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ConfiguracionTiendaRepository extends JpaRepository<ConfiguracionTienda, UUID> {

    
}
