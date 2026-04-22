package com.jugamir.backend.repository;

import com.jugamir.backend.model.JugadorPartida;
import com.jugamir.backend.model.Partida;
import com.jugamir.backend.model.enums.ResultadoJugador;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JugadorPartidaRepository extends JpaRepository<JugadorPartida, Long> {

    // Obtiene todos los jugadores de una partida
    List<JugadorPartida> findByPartida(Partida partida);

    // Cuenta el número de jugadores de una partida
    int countByPartida(Partida partida);

    // Obtiene un jugador de una partida por el id del usuario
    Optional<JugadorPartida> findByPartidaAndJugador_Usuario_IdUsuario(Partida partida, Long usuarioId);

    // Obtiene el numero de jugadores excluyendo los que tienen
    // resultado='resultado'
    int countByPartidaAndResultadoNot(Partida partida, ResultadoJugador resultado);

    // Obtiene el numero de jugadores que tienen resultado='resultado'
    int countByPartidaAndResultado(Partida partida, ResultadoJugador resultado);
}
