package com.jugamir.backend.dto;

import com.jugamir.backend.model.Categoria;
import com.jugamir.backend.model.Pregunta;

public record GirarRuletaResponse(
        Categoria categoria,
        Pregunta pregunta) {

}
