package com.ecommerce.backend.tienda.repository;
import com.ecommerce.backend.tienda.model.Plantilla;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface PlantillaRepository extends JpaRepository<Plantilla, Short> {
    Optional<Plantilla> findByNombre(String nombre); //Buscar plantilla por nombre
   
    
}
