package com.jugamir.backend.dto;

import jakarta.validation.constraints.NotNull;

public record ResponderPreguntaRequest(

                @NotNull Long respuestaId,
                int tiempoMs) {

}
