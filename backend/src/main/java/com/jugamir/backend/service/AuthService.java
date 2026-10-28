package com.jugamir.backend.service;

import com.jugamir.backend.dto.*;
import com.jugamir.backend.exception.BusinessException;
import com.jugamir.backend.model.*;
import com.jugamir.backend.repository.*;
import com.jugamir.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final com.jugamir.backend.security.AccountLock accounts;
    private final LoginChallengeService challenges;
    private final com.jugamir.backend.security.AuthRateLimiter limiter;
    private final com.jugamir.backend.security.ProfessorRegistrationPolicy professorRegistrationPolicy;
    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final ProfesorRepository profesorRepository;
    private final PasswordEncoder passwordEncoder;
    private final Codigo2faRepository codigo2faRepository;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // Contenedor que devuelve verify(): el AuthResponse (sin refreshToken)
    // y el refreshToken para que el controlador lo inyecte en la cookie
    public record VerifyResult(AuthResponse authResponse, String refreshToken) {
    }

    @Transactional
    public void registerJugador(RegisterJugadorRequest request) {

        // Validamos que el usuario no exista
        validarUsuario(request);

        if (jugadorRepository.existsByNick(request.getNick())) {
            throw new BusinessException("Ya existe un usuario con ese nick");
        }

        // Creamos el Usuario base
        Usuario usuario = guardarUsuario(request);

        jugadorRepository.save(Jugador.builder()
                .usuario(usuario)
                .nick(request.getNick())
                .build());

    }

    @Transactional
    public void registerProfesor(RegisterProfesorRequest request) {
        professorRegistrationPolicy.requireInstitutionalEmail(request.getEmail());

        validarUsuario(request);

        Usuario usuario = guardarUsuario(request);

        profesorRepository.save(Profesor.builder()
                .usuario(usuario)
                .departamento(request.getDepartamento())
                .build());

    }

    @Transactional
    public String login(LoginRequest request) {
        limiter.check("login-account", request.email(), 10);

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Las credenciales no coinciden con nuestros registros"));

        usuario = accounts.current(usuario.getIdUsuario());
        if (!passwordEncoder.matches(request.password(), usuario.getContrasenaHash()))
            throw new BusinessException("Las credenciales no coinciden con nuestros registros");

        return challenges.issue(usuario);
    }

    @Transactional
    public VerifyResult verify(VerifyRequest request) {

        var identity = challenges.consume(request.challengeId(), request.codigo());
        Usuario usuario = accounts.current(identity.userId());

        if (usuario.getTokenVersion() != identity.tokenVersion())
            throw new org.springframework.security.authentication.BadCredentialsException("Inicia sesión de nuevo");
        usuario.setEsActivo(true);
        usuarioRepository.save(usuario);

        // Determinar qué tipo de usuario es y obtener su nick si es jugador
        String rol = jugadorRepository.existsByUsuario(usuario) ? "JUGADOR" : "PROFESOR";
        String nick = jugadorRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .map(j -> j.getNick())
                .orElse("");

        // El JWT incluye la versión actual. Si el usuario hace logout, la versión
        // sube y este JWT queda inválido en la siguiente petición de cualquier
        // dispositivo.
        String jwt = jwtService.generateToken(usuario.getEmail(), usuario.getTokenVersion(), rol);

        // Si el usuario tiene sesion iniciada en otro dispositivo, se le asigna ese
        // token
        // Si no, se crea uno nuevo
        Optional<RefreshToken> rtExistente = refreshTokenRepository.findByUsuario(usuario);
        if (rtExistente.isPresent() && !rtExistente.get().getExpiraEn().isAfter(LocalDateTime.now())) {
            refreshTokenRepository.delete(rtExistente.get());
            refreshTokenRepository.flush();
            rtExistente = Optional.empty();
        }
        RefreshToken rt = rtExistente.orElseGet(() -> refreshTokenRepository.save(
                RefreshToken.builder()
                        .token(UUID.randomUUID().toString())
                        .usuario(usuario)
                        .expiraEn(LocalDateTime.now().plusDays(30))
                        .build()));

        // El refreshToken se envia en la cookie, nunca en el body
        return new VerifyResult(
                new AuthResponse(jwt, usuario.getEmail(), nick, rol, usuario.getNombre(), usuario.getIdUsuario()),
                rt.getToken());
    }

    @Transactional
    public Map<String, String> renovarToken(String refreshToken) {

        RefreshToken rt = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException("Token no encontrado"));

        if (rt.getExpiraEn().isBefore(LocalDateTime.now()))
            throw new BusinessException("El token ha expirado");

        Usuario usuario = accounts.current(rt.getUsuario().getIdUsuario());
        if (refreshTokenRepository.findByToken(refreshToken).isEmpty())
            throw new org.springframework.security.authentication.BadCredentialsException("Sesión caducada");
        String rol = jugadorRepository.existsByUsuario(usuario) ? "JUGADOR" : "PROFESOR";
        String nuevoJwt = jwtService.generateToken(usuario.getEmail(), usuario.getTokenVersion(), rol);

        return Map.of("token", nuevoJwt);
    }

    @Transactional
    public void logout(String refreshToken) {

        RefreshToken rt = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException("Token no encontrado"));

        Usuario usuario = accounts.current(rt.getUsuario().getIdUsuario());
        if (refreshTokenRepository.findByToken(refreshToken).isEmpty())
            throw new org.springframework.security.authentication.BadCredentialsException("Sesión caducada");
        // Al subir la versión, todos los JWT con versión anterior son rechazados
        // inmediatamente en la siguiente petición
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        usuarioRepository.save(usuario);

        refreshTokenRepository.deleteByUsuario(usuario);
        codigo2faRepository.deleteByUsuario(usuario);
    }

    private void validarUsuario(RegisterRequest request) {
        limiter.check("register-account", request.getEmail(), 3);
        if (usuarioRepository.existsByEmail(request.getEmail()))
            throw new BusinessException("Ya existe un usuario con ese email");
    }

    private Usuario guardarUsuario(RegisterRequest request) {
        return usuarioRepository.save(Usuario.builder()
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .email(request.getEmail())
                .contrasenaHash(passwordEncoder.encode(request.getPassword()))
                .esActivo(true)
                .creadoEn(LocalDateTime.now())
                .build());
    }

    public void reenviarCodigo(String challengeId) {
        challenges.resend(challengeId);
    }

    @Transactional
    public void contrasenaOlvidada(ForgotPasswordRequest request) {
        limiter.check("reset-account", request.email(), 3);

        Usuario usuario = usuarioRepository.findByEmail(request.email()).orElse(null);
        if (usuario == null) return;
        usuario = accounts.current(usuario.getIdUsuario());

        passwordResetTokenRepository.deleteByUsuario(usuario);

        String token = java.util.UUID.randomUUID().toString();

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .token(token)
                .usuario(usuario)
                .expiraEn(LocalDateTime.now().plusMinutes(15))
                .usado(false)
                .build());

        emailService.enviarEnlaceCambioContrasena(usuario.getEmail(), token);
    }

    @Transactional
    public void resetearContrasena(ResetPasswordRequest request) {

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsadoFalse(request.token())
                .orElseThrow(() -> new BusinessException("Token inválido o ya usado"));

        if (resetToken.getExpiraEn().isBefore(LocalDateTime.now()))
            throw new BusinessException("El enlace ha expirado");

        Usuario usuario = accounts.current(resetToken.getUsuario().getIdUsuario());
        if (passwordResetTokenRepository.consume(request.token(), LocalDateTime.now()) != 1)
            throw new BusinessException("Token inválido o ya usado");
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        refreshTokenRepository.deleteByUsuario(usuario);
        codigo2faRepository.deleteByUsuario(usuario);
        usuario.setContrasenaHash(passwordEncoder.encode(request.nuevaPassword()));
        usuarioRepository.save(usuario);

        passwordResetTokenRepository.deleteByUsuario(usuario);
    }
}
