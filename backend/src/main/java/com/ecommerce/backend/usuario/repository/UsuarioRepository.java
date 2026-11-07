package com.ecommerce.backend.usuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ecommerce.backend.usuario.model.UsuarioPlataforma;
import java.util.UUID;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository< UsuarioPlataforma, UUID> {
    
    //Nos ayudara para el login para saber si existe o no un usuario con el mismo correo
    Optional<UsuarioPlataforma>findByCorreoIgnoreCase(String correo);

    //Nos ayudara para el registro para saber si existe o no un usuario con el mismo correo
    boolean existsByCorreoIgnoreCase(String correo);
}
