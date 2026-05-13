package com.jugamir.backend.dto;

import com.jugamir.backend.model.enums.Dificultad;
import com.jugamir.backend.model.enums.TipoPartida;
import java.util.List;

public record CrearPartidaRequest(
        TipoPartida tipo,
        List<Dificultad> dificultades,
        int tiempoRespuesta,
        int maxJugadores,
        List<Long> categoriaIds,
        int aciertosParaQuesito) {
}
