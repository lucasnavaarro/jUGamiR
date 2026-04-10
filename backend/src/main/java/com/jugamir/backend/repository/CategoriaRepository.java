package com.jugamir.backend.repository;

import com.jugamir.backend.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByNombre(String nombre);

    Optional<Categoria> findByColor(String color);
}
