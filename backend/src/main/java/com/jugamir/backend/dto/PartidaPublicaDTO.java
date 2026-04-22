package com.jugamir.backend.dto;

public record PartidaPublicaDTO(
        Long id,
        String anfitrion,
        String emailCreador,
        int jugadoresActuales,
        int maxJugadores,
        String dificultad) {

}
