package com.jugamir.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import com.jugamir.backend.security.PasswordPolicy;

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
    @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
    private String password;

}
