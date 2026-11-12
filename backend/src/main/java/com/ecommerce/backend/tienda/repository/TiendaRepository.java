package com.ecommerce.backend.tienda.repository;
import com.ecommerce.backend.tienda.model.Tienda;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface TiendaRepository extends JpaRepository<Tienda, UUID> {
    boolean existsByPropietario_IdUsuario(UUID idUsuario); //Verifica si ya tiene alguna tienda 
    boolean existsBySlug(String slug); //Verificar si un slug ya existe
    List<Tienda> findByNombreContainingIgnoreCase(String nombre); //Buscar tiendas por nombre (sin importar mayúsculas/minúsculas)
    Optional<Tienda> findBySlug(String slug); //Buscar tienda por slug
    
    List<Tienda> findByPropietario_IdUsuario(UUID idUsuario); //Buscar tiendas por propietario
}
