package com.jugamir.backend.dto;

import com.jugamir.backend.model.Categoria;
import java.util.List;
import java.util.Map;

public record GirarRuletaResponse(
                Categoria categoria,
                PreguntaDTO pregunta,
                List<Map<String, Object>> respuestas) {

}
