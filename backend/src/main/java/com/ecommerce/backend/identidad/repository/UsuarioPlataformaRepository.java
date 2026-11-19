package com.ecommerce.backend.identidad.repository;
import com.ecommerce.backend.identidad.model.UsuarioPlataforma;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioPlataformaRepository extends JpaRepository<UsuarioPlataforma, UUID> {

    @org.springframework.data.jpa.repository.Query("""
        SELECT u FROM UsuarioPlataforma u
        WHERE :texto = '' OR LOWER(u.nombres) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :texto, '%'))
        """)
    org.springframework.data.domain.Page<UsuarioPlataforma> buscarAdministracion(
            @org.springframework.data.repository.query.Param("texto") String texto,
            org.springframework.data.domain.Pageable pageable);

    boolean existsByEmail(String email); //Registro verificar si existe email
    Optional<UsuarioPlataforma> findByEmail(String email); //login buscar usuario por email
    
}
