package com.jugamir.backend.dto;

import com.jugamir.backend.model.enums.Dificultad;
import com.jugamir.backend.model.enums.EstadoPregunta;

public record PreguntaDTO(
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
        Long asignaturaId) {
}
