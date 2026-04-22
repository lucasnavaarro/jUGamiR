package com.jugamir.backend.dto;

import com.jugamir.backend.model.enums.Dificultad;
import com.jugamir.backend.model.enums.TipoPartida;

public record CrearPartidaRequest(
        TipoPartida tipo,
        Dificultad dificultad,
        int tiempoRespuesta,
        int maxJugadores

) {
}
