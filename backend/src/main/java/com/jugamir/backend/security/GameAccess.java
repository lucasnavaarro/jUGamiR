package com.jugamir.backend.security;

import com.jugamir.backend.repository.JugadorPartidaRepository;
import com.jugamir.backend.repository.PartidaRepository;
import com.jugamir.backend.model.Partida;
import com.jugamir.backend.model.enums.ResultadoJugador;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GameAccess {
    private final PartidaRepository partidas;
    private final JugadorPartidaRepository jugadores;

    @Transactional(readOnly = true)
    public Partida requireMember(Long partidaId, Long usuarioId) {
        Partida partida = partidas.findById(partidaId).orElseThrow(() -> new AccessDeniedException("No tienes acceso a esta partida"));
        var jugador = jugadores.findByPartidaAndJugador_Usuario_IdUsuario(partida, usuarioId)
                .orElseThrow(() -> new AccessDeniedException("No tienes acceso a esta partida"));
        if (jugador.getResultado() == ResultadoJugador.EXPULSADO || jugador.getResultado() == ResultadoJugador.ABANDONADA)
            throw new AccessDeniedException("Ya no perteneces a esta partida");
        return partida;
    }
}
