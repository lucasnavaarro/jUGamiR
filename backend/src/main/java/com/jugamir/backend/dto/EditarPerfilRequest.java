package com.jugamir.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EditarPerfilRequest(
                @NotBlank(message = "El nombre es obligatorio") String nombre,
                @NotBlank(message = "Los apellidos son obligatorios") String apellidos,
                @NotBlank(message = "El email es obligatorio") @Email(message = "El email debe ser válido") String email,
                String nick, // null si es profesor
                String departamento // null si es jugador
) {

}
