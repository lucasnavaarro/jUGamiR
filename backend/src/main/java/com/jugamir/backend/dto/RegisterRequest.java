package com.jugamir.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellidos;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String dni;

    @NotBlank
    @Size(min = 8, message = "La contraseña dene tener al menos 8 caractreres")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).{8,}$", message = "La contraseña debe contener al menos una mayúscula, una minúscula, un número y un carácter especial")
    private String password;

}
