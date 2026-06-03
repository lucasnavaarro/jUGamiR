package com.jugamir.backend.repository;

import com.jugamir.backend.model.Pregunta;
import com.jugamir.backend.model.enums.Dificultad;
import com.jugamir.backend.model.enums.EstadoPregunta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

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

        @Query("SELECT p FROM Pregunta p " +
                        "WHERE p.asignatura.categoria.id = :categoriaId " +
                        "AND p.estado = :estado " +
                        "AND p.dificultad = :dificultad " +
                        "AND p.anulada = false " +
                        "AND p.id NOT IN :usadas " +
                        "ORDER BY FUNCTION('RANDOM') " +
                        "LIMIT :limite")
        List<Pregunta> findAleatoriasByCategoriaYDificultad(
                        @Param("categoriaId") Long categoriaId,
                        @Param("estado") EstadoPregunta estado,
                        @Param("dificultad") Dificultad dificultad,
                        @Param("usadas") List<Long> usadas,
                        @Param("limite") int limite);

        // Solo para pruebas — forzar preguntas con imagen
        @Query("SELECT p FROM Pregunta p WHERE p.imagenUrl IS NOT NULL AND p.imagenUrl != '' AND p.estado = 'PUBLICADA' AND p.anulada = false ORDER BY FUNCTION('RANDOM') LIMIT :limite")
        List<Pregunta> findAleatoriasConImagen(@Param("limite") int limite);

        @Query("SELECT p FROM Pregunta p " +
                        "WHERE p.asignatura.categoria.id = :categoriaId " +
                        "AND p.estado = :estado " +
                        "AND p.dificultad IN :dificultades " +
                        "AND p.anulada = false " +
                        "AND p.id NOT IN :usadas " +
                        "ORDER BY FUNCTION('RANDOM') " +
                        "LIMIT :limite")
        List<Pregunta> findAleatoriasByCategoriaYDificultades(
                        @Param("categoriaId") Long categoriaId,
                        @Param("estado") EstadoPregunta estado,
                        @Param("dificultades") List<Dificultad> dificultades,
                        @Param("usadas") List<Long> usadas,
                        @Param("limite") int limite);

        boolean existsByIdentificador(String identificador);

        Optional<Pregunta> findByIdentificador(String identificador);

        @Query("SELECT p FROM Pregunta p WHERE " +
                        "LOWER(p.identificador) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
                        "LOWER(p.enunciado) LIKE LOWER(CONCAT('%', :q, '%')) " +
                        "ORDER BY p.identificador")
        Page<Pregunta> buscarPorIdentificadorOEnunciado(@Param("q") String q, Pageable pageable);

        boolean existsByEnunciado(String enunciado);

        boolean existsByCreadaPor(com.jugamir.backend.model.Usuario usuario);

}
