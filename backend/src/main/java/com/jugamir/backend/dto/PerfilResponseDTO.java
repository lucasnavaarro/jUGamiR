package com.jugamir.backend.dto;

public record PerfilResponseDTO(
        String nombre,
        String apellidos,
        String email,
        String rol,
        String nick, // null si es profesor
        String departamento // null si es jugador
) {

}
