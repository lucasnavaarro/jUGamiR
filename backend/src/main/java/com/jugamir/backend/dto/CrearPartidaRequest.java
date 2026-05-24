package com.jugamir.backend.dto;

import com.jugamir.backend.model.enums.Dificultad;
import com.jugamir.backend.model.enums.TipoPartida;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotNull;

public record CrearPartidaRequest(
        @NotNull TipoPartida tipo,
        @NotNull List<Dificultad> dificultades,
        int tiempoRespuesta,
        int maxJugadores,
        @NotNull List<Long> categoriaIds,
        int aciertosParaQuesito,
        boolean modoEntrenamiento,
        Map<Long, Integer> categoriaPesos) {
}
