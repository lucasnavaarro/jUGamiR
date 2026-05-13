package com.jugamir.backend.dto;

import java.util.List;

public record PartidaPublicaDTO(
                Long id,
                String anfitrion,
                String emailCreador,
                int jugadoresActuales,
                int maxJugadores,
                List<String> dificultades) {

}
