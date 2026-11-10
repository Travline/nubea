package com.ecommerce.backend.identidad.repository;
import com.ecommerce.backend.identidad.model.UsuarioIdentidad;
import com.ecommerce.backend.identidad.model.ProveedorIdentidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioIdentidadRepository extends JpaRepository<UsuarioIdentidad, UUID> {
    Optional<UsuarioIdentidad> findByUsuario_EmailAndProveedor(String email, ProveedorIdentidad proveedor);
    
}
