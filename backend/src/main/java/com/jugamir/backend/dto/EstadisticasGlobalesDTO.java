package com.jugamir.backend.dto;

import java.util.List;

public record EstadisticasGlobalesDTO(
        int partidasJugadas,
        int victorias,
        double porcentajeVictorias,
        int totalRespondidas,
        int totalAciertos,
        int totalFallos,
        int totalNoRespondidas,
        double porcentajeAciertos,
        double tiempoMedioMs,
        List<EstadisticasCategoriaDTO> categorias) {

}
