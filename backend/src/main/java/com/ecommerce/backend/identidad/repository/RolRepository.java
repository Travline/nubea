package com.ecommerce.backend.identidad.repository;
import com.ecommerce.backend.identidad.model.Rol;
import com.ecommerce.backend.identidad.model.NombreRol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Short> {
    Optional<Rol> findByNombre(NombreRol nombre); //Buscar rol por nombre 
    
}
