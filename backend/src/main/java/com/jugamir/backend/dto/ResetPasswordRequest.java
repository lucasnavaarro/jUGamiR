package com.jugamir.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.jugamir.backend.security.PasswordPolicy;

public record ResetPasswordRequest(
                @NotBlank(message = "El token es obligatorio") String token,
                @NotBlank(message = "La nueva contraseña es obligatoria")
                @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE) String nuevaPassword) {
}
