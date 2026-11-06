package com.jugamir.backend.dto;

import java.util.List;

public record PartidaPublicaDTO(
                Long id,
                String anfitrion,
                int jugadoresActuales,
                int maxJugadores,
                List<String> dificultades) {

}
