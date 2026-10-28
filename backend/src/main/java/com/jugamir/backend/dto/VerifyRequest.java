package com.jugamir.backend.dto;
import jakarta.validation.constraints.*;
public record VerifyRequest(@NotBlank @Size(max = 36) String challengeId,
                            @NotBlank @Pattern(regexp = "[0-9]{6}") String codigo) {}
