package com.jugamir.backend.dto;

public record PuntoProgresoCategoriaDTO(
        String fecha,
        double pctAcierto,
        double tiempoMedioMs) {

}
