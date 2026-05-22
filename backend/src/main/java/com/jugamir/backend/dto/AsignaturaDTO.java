package com.jugamir.backend.dto;

public record AsignaturaDTO(
        Long id,
        String nombre,
        Long categoriaId,
        String categoriaNombre) {

}
