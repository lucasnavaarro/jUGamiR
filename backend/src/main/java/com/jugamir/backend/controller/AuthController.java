package com.jugamir.backend.controller;

import com.jugamir.backend.dto.*;
import com.jugamir.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.jugamir.backend.security.RefreshCookiePolicy cookies;

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
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {

        return ResponseEntity.accepted().body(Map.of("challengeId", authService.login(request)));
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verify(@Valid @RequestBody VerifyRequest request) {

        AuthService.VerifyResult result = authService.verify(request);

        // EL REFRESH TOKEN VIAJA EN UNA COOKIE
        ResponseCookie cookie = cookies.create(result.refreshToken(), Duration.ofDays(30));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.authResponse());

    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank())
            return ResponseEntity.status(401).build();

        return ResponseEntity.ok(authService.renovarToken(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken) {

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // Destruye la cookie en el navegador: maxAge(0) hace que caduque inmediatamente
        ResponseCookie cookieBorrada = cookies.create("", Duration.ZERO);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieBorrada.toString())
                .build();
    }

    @PostMapping("/reenviar-codigo")
    public ResponseEntity<Void> reenviarCodigo(@Valid @RequestBody ResendCodeRequest body) {
        authService.reenviarCodigo(body.challengeId());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.contrasenaOlvidada(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetearContrasena(request);
        return ResponseEntity.ok().build();
    }

    // Endpoint ligero para validar que el JWT sigue siendo válido.
    // Spring Security ya valida el JWT antes de llegar aquí:
    // si el token es inválido o tiene tokenVersion viejo → 401 automático.
    @GetMapping("/me")
    public ResponseEntity<Void> me() {
        return ResponseEntity.ok().build();
    }

}
