package com.jugamir.backend.dto;

public record EstadisticasCategoriaDTO(
        Long categoriaId,
        String nombre,
        String color,
        long total,
        long aciertos,
        long fallos,
        double porcentajeAcierto,
        double tiempoMedioMs) {

}
