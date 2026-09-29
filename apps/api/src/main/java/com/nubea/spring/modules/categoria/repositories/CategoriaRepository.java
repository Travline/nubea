package com.nubea.spring.modules.categoria.repositories;

import com.nubea.spring.modules.categoria.entities.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}