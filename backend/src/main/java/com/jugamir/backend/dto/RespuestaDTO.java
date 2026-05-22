package com.jugamir.backend.dto;

public record RespuestaDTO(
        Long id,
        String texto,
        boolean esCorrecta,
        int orden) {

}
