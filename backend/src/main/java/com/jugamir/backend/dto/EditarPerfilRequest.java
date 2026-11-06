package com.jugamir.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EditarPerfilRequest(
                @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
                @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 120) String apellidos,
                @NotBlank(message = "El email es obligatorio") @Email(message = "El email debe ser válido") @Size(max = 255) String email,
                @Size(max = 80) String nick, // null si es profesor
                @Size(max = 120) String departamento, // null si es jugador
                String currentPassword,
                String emailCode
) {

    public EditarPerfilRequest { email = email == null ? null : email.strip().toLowerCase(java.util.Locale.ROOT); nick = nick == null ? null : nick.strip(); }
}
