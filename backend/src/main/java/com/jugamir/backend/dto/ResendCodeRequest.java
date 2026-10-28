package com.jugamir.backend.dto;
import jakarta.validation.constraints.*;
public record ResendCodeRequest(@NotBlank @Size(max = 36) String challengeId) {}
