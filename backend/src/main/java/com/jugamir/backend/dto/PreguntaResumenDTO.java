package com.jugamir.backend.dto;

import com.jugamir.backend.model.enums.Dificultad;
import com.jugamir.backend.model.enums.EstadoPregunta;
import java.util.List;

public record PreguntaResumenDTO(
        Long id,
        String identificador,
        String tituloIndice,
        String enunciado,
        String imagenUrl,
        Integer anio,
        String comentario,
        boolean anulada,
        Dificultad dificultad,
        EstadoPregunta estado,
        Long asignaturaId,
        String asignaturaNombre,
        List<RespuestaDTO> respuestas) {

}
