package com.jugamir.backend.dto;

public record ResetPasswordRequest(
        String token,
        String nuevaPassword
) {}
