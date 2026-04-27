package com.jugamir.backend.repository;

import com.jugamir.backend.model.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    // Para que el jugador se una con el código de la partida
    Optional<Partida> findByCodigoUnion(String codigoUnion);

    // Listar partidas públicas — CAST explícito necesario para enums nativos de PostgreSQL
    @Query(value = "SELECT * FROM partidas WHERE tipo = CAST(:tipo AS tipo_partida) AND estado = CAST(:estado AS estado_partida)", nativeQuery = true)
    List<Partida> findByTipoAndEstado(@Param("tipo") String tipo, @Param("estado") String estado);

}
