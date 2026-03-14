package com.jugamir.backend.controller;

import com.jugamir.backend.dto.*;
import com.jugamir.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/jugador")
    public ResponseEntity<Void> registerJugador(@Valid @RequestBody RegisterJugadorRequest request) {
        authService.registerJugador(request);
        return ResponseEntity.accepted().build();

    }

    @PostMapping("/register/profesor")
    public ResponseEntity<Void> registerProfesor(@Valid @RequestBody RegisterProfesorRequest request) {
        authService.registerProfesor(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {

        authService.login(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verify(@Valid @RequestBody VerifyRequest request) {

        return ResponseEntity.ok(authService.verify(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.contrasenaOlvidada(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetearContrasena(request);
        return ResponseEntity.ok().build();
    }

}
