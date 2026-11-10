package com.ecommerce.backend.identidad.repository;
import com.ecommerce.backend.identidad.model.UsuarioPlataforma;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioPlataformaRepository extends JpaRepository<UsuarioPlataforma, UUID> {

    boolean existsByEmail(String email); //Registro verificar si existe email
    Optional<UsuarioPlataforma> findByEmail(String email); //login buscar usuario por email
    
}
