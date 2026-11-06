package com.jugamir.backend.repository;

import com.jugamir.backend.model.Asignatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsignaturaRepository extends JpaRepository<Asignatura, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Asignatura a where a.id = :id")
    Optional<Asignatura> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Asignatura> findByNombreIgnoreCase(String nombre);

    List<Asignatura> findAllByOrderByCategoria_NombreAscNombreAsc();

    List<Asignatura> findByCategoria_Id(Long categoriaId);

}
