package com.jugamir.backend.dto;

public record EstadisticasAsignaturasDTO(
        Long asignaturaId,
        String nombre,
        long total,
        long aciertos,
        long fallos,
        double porcentajeAcierto,
        double tiempoMedioMs) {

}
