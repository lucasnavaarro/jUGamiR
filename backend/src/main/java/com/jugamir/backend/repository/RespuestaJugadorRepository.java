package com.jugamir.backend.repository;

import com.jugamir.backend.model.RespuestaJugador;
import com.jugamir.backend.model.JugadorPartida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RespuestaJugadorRepository extends JpaRepository<RespuestaJugador, Long> {

    // Obtiene las respuestas de un jugador en una partida
    List<RespuestaJugador> findByJugadorPartida(JugadorPartida jugadorPartida);

    List<RespuestaJugador> findByJugadorPartida_Jugador_Usuario_IdUsuario(Long usuarioId);
}
