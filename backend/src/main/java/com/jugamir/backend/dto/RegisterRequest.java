package com.jugamir.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import com.jugamir.backend.security.PasswordPolicy;

@Data
public class RegisterRequest {

    @NotBlank @Size(max=120)
    private String nombre;

    @NotBlank @Size(max=120)
    private String apellidos;

    @Email @Size(max=255)
    @NotBlank
    private String email;

    @NotBlank
    @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
    private String password;

    public void setEmail(String email) { this.email = email == null ? null : email.strip().toLowerCase(java.util.Locale.ROOT); }
}
