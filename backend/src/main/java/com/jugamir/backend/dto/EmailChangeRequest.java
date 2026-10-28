package com.jugamir.backend.dto;
import jakarta.validation.constraints.*;
public record EmailChangeRequest(@NotBlank @Email @Size(max=255) String email,
                                 @NotBlank @Size(max=256) String currentPassword) {}
