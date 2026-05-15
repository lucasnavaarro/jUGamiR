package com.jugamir.backend.dto;

public record PuntoProgresoDTO(
        String fecha,
        double pctVictorias,
        double pctAcierto,
        double tiempoMedioMs) {

}
