package com.jugamir.backend.repository;

import com.jugamir.backend.model.Asignatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsignaturaRepository extends JpaRepository<Asignatura, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Asignatura> findByNombreIgnoreCase(String nombre);

    List<Asignatura> findAllByOrderByCategoria_NombreAscNombreAsc();

    List<Asignatura> findByCategoria_Id(Long categoriaId);

}