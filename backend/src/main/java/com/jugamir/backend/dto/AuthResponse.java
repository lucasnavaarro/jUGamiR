package com.jugamir.backend.dto;

public record AuthResponse(
                String token,
                String email,
                String nick,
                String rol,
                String nombre) {
}
