package com.jugamir.backend.repository;

import com.jugamir.backend.model.Pregunta;
import com.jugamir.backend.model.enums.EstadoPregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {

        @Query("SELECT p FROM Pregunta p"
                        + " WHERE p.asignatura.id = :asignaturaId"
                        + " AND p.estado = :estado"
                        + " AND p.anulada = false"
                        + " ORDER BY FUNCTION('RANDOM')"
                        + " LIMIT :limite")
        List<Pregunta> findAleatoriasByAsignatura(
                        @Param("asignaturaId") Long asignaturaId,
                        @Param("estado") EstadoPregunta estado,
                        @Param("limite") int limite);

        @Query("SELECT p FROM Pregunta p " +
                        "WHERE p.asignatura.categoria.id = :categoriaId " +
                        "AND p.estado = :estado " +
                        "AND p.anulada = false " +
                        "AND p.id NOT IN :usadas " +
                        "ORDER BY FUNCTION('RANDOM') " +
                        "LIMIT :limite")
        List<Pregunta> findAleatoriasByCategoria(
                        @Param("categoriaId") Long categoriaId,
                        @Param("estado") EstadoPregunta estado,
                        @Param("usadas") List<Long> usadas,
                        @Param("limite") int limite);
}
