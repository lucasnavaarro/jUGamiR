package com.jugamir.backend.dto;

public record PuntoProgresoDTO(
        String fecha,
        double pctVictorias,
        double pctAcierto,
        double tiempoMedioMs,
        String periodo) {
    public PuntoProgresoDTO(String fecha, double pctVictorias, double pctAcierto, double tiempoMedioMs) { this(fecha, pctVictorias, pctAcierto, tiempoMedioMs, "día"); }
}
