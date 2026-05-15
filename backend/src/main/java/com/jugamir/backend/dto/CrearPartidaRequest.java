package com.jugamir.backend.dto;

import com.jugamir.backend.model.enums.Dificultad;
import com.jugamir.backend.model.enums.TipoPartida;
import java.util.List;
import java.util.Map;

public record CrearPartidaRequest(
                TipoPartida tipo,
                List<Dificultad> dificultades,
                int tiempoRespuesta,
                int maxJugadores,
                List<Long> categoriaIds,
                int aciertosParaQuesito,
                boolean modoEntrenamiento,
                Map<Long, Integer> categoriaPesos) {
}
