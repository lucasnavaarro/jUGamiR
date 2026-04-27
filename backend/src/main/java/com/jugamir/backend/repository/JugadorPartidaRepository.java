package com.jugamir.backend.repository;

import com.jugamir.backend.model.JugadorPartida;
import com.jugamir.backend.model.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface JugadorPartidaRepository extends JpaRepository<JugadorPartida, Long> {

    // Obtiene todos los jugadores de una partida
    List<JugadorPartida> findByPartida(Partida partida);

    // Cuenta el número de jugadores de una partida
    int countByPartida(Partida partida);

    // Obtiene un jugador de una partida por el id del usuario
    Optional<JugadorPartida> findByPartidaAndJugador_Usuario_IdUsuario(Partida partida, Long usuarioId);

    // Cuenta los jugadores con resultado != 'resultado' — CAST necesario para enum nativo de PostgreSQL
    @Query(value = "SELECT COUNT(*) FROM jugadores_partida WHERE partida_id = :#{#partida.id} AND resultado != CAST(:resultado AS resultado_jugador)", nativeQuery = true)
    int countByPartidaAndResultadoNot(@Param("partida") Partida partida, @Param("resultado") String resultado);

    // Cuenta los jugadores con resultado = 'resultado' — CAST necesario para enum nativo de PostgreSQL
    @Query(value = "SELECT COUNT(*) FROM jugadores_partida WHERE partida_id = :#{#partida.id} AND resultado = CAST(:resultado AS resultado_jugador)", nativeQuery = true)
    int countByPartidaAndResultado(@Param("partida") Partida partida, @Param("resultado") String resultado);
}
