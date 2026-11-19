package com.ecommerce.backend.identidad.repository;
import com.ecommerce.backend.identidad.model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Short>    {
    Optional<TipoDocumento> findByNombre(String nombre); //Buscar tipo de documento por nombre
    Optional<TipoDocumento> findByCodigo(String codigo); //Buscar tipo de documento por codigo
}
