package com.jugamir.backend.dto;

import com.jugamir.backend.model.enums.Dificultad;
import com.jugamir.backend.model.enums.TipoPartida;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.*;

public record CrearPartidaRequest(
        @NotNull TipoPartida tipo,
        @NotEmpty List<@NotNull Dificultad> dificultades,
        @jakarta.validation.constraints.Min(5) @jakarta.validation.constraints.Max(600) int tiempoRespuesta,
        @Min(1) @Max(6) int maxJugadores,
        @NotEmpty List<@NotNull @Positive Long> categoriaIds,
        @Min(1) @Max(30) int aciertosParaQuesito,
        boolean modoEntrenamiento,
        Map<@NotNull @Positive Long, @NotNull @Positive Integer> categoriaPesos) {
}
