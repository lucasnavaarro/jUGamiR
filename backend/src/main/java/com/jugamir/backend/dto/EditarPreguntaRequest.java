package com.jugamir.backend.dto;

import com.jugamir.backend.model.enums.Dificultad;
import java.util.List;

public record EditarPreguntaRequest(
        String identificador,
        String tituloIndice,
        String enunciado,
        String imagenUrl,
        String comentario,
        boolean anulada,
        Dificultad dificultad,
        Long asignaturaId,
        List<RespuestaDTO> respuestas) {

}
