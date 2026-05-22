package com.jugamir.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearAsignaturaRequest(
        @NotBlank(message = "El nombre de la asignatura es obligatorio") String nombre,
        @NotNull(message = "La categoría de la asignatura es obligatoria") Long categoriaId) {

}
