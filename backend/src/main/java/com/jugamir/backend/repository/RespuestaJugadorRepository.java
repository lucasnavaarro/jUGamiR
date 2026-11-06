package com.jugamir.backend.repository;

import com.jugamir.backend.model.RespuestaJugador;
import com.jugamir.backend.model.JugadorPartida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RespuestaJugadorRepository extends JpaRepository<RespuestaJugador, Long> {

    // Obtiene las respuestas de un jugador en una partida
    List<RespuestaJugador> findByJugadorPartida(JugadorPartida jugadorPartida);

    // Precarga en una sola consulta toda la cadena de relaciones que StatsService
    // necesita recorrer (preguntaPartida -> pregunta -> asignatura -> categoria,
    // jugadorPartida -> partida), para evitar N+1 queries por respuesta
    @Query("SELECT rj FROM RespuestaJugador rj " +
            "JOIN FETCH rj.preguntaPartida pp " +
            "JOIN FETCH pp.pregunta p " +
            "JOIN FETCH p.asignatura a " +
            "JOIN FETCH a.categoria " +
            "JOIN FETCH rj.jugadorPartida jp " +
            "JOIN FETCH jp.partida " +
            "WHERE jp.jugador.usuario.idUsuario = :usuarioId")
    List<RespuestaJugador> findByJugadorPartida_Jugador_Usuario_IdUsuarioConDatos(@Param("usuarioId") Long usuarioId);
}
