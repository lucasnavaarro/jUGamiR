package com.jugamir.backend.dto;

public record PuntoProgresoCategoriaDTO(
        String fecha,
        double pctAcierto,
        double tiempoMedioMs,
        String periodo) {
    public PuntoProgresoCategoriaDTO(String fecha, double pctAcierto, double tiempoMedioMs) { this(fecha, pctAcierto, tiempoMedioMs, "día"); }
}
