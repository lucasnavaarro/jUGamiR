package com.jugamir.backend.service;

import com.jugamir.backend.dto.*;
import com.jugamir.backend.model.*;
import com.jugamir.backend.repository.*;
import com.jugamir.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
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
            throw new RuntimeException("Ya existe un usuario con ese nick");
        }

        // Creamos el Usuario base
        Usuario usuario = guardarUsuario(request);

        jugadorRepository.save(Jugador.builder()
                .usuario(usuario)
                .nick(request.getNick())
                .build());

        generarYEnviarCodigo(usuario);

    }

    @Transactional
    public void registerProfesor(RegisterProfesorRequest request) {

        validarUsuario(request);

        Usuario usuario = guardarUsuario(request);

        profesorRepository.save(Profesor.builder()
                .usuario(usuario)
                .departamento(request.getDepartamento())
                .build());

        generarYEnviarCodigo(usuario);

    }

    @Transactional
    public void login(LoginRequest request) {

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Las credenciales no coinciden con nuestros registros"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getContrasenaHash()))
            throw new RuntimeException("Las credenciales no coinciden con nuestros registros");

        generarYEnviarCodigo(usuario);
    }

    @Transactional
    public VerifyResult verify(VerifyRequest request) {

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Codigo2fa codigo = codigo2faRepository.findByUsuarioAndCodigoAndUsadoFalse(usuario, request.getCodigo())
                .orElseThrow(() -> new RuntimeException("Código incorrecto o ya usado"));

        if (codigo.getExpiraEn().isBefore(LocalDateTime.now()))
            throw new RuntimeException("El código ya ha expirado");

        codigo.setUsado(true);
        codigo2faRepository.save(codigo);

        usuario.setEsActivo(true);
        usuarioRepository.save(usuario);

        // Determinar qué tipo de usuario es
        String rol = jugadorRepository.existsByUsuario(usuario) ? "JUGADOR" : "PROFESOR";

        // El JWT incluye la versión actual. Si el usuario hace logout, la versión
        // sube y este JWT queda inválido en la siguiente petición de cualquier
        // dispositivo.
        String jwt = jwtService.generateToken(usuario.getEmail(), usuario.getTokenVersion());

        // Si el usuario tiene sesion iniciada en otro dispositivo, se le asigna ese
        // token
        // Si no, se crea uno nuevo
        Optional<RefreshToken> rtExistente = refreshTokenRepository.findByUsuario(usuario);
        RefreshToken rt = rtExistente.orElseGet(() -> refreshTokenRepository.save(
                RefreshToken.builder()
                        .token(UUID.randomUUID().toString())
                        .usuario(usuario)
                        .expiraEn(LocalDateTime.now().plusDays(30))
                        .build()));

        // El refreshToken se envia en la cookie, nunca en el body
        return new VerifyResult(
                new AuthResponse(jwt, usuario.getEmail(), "", rol),
                rt.getToken());
    }

    @Transactional
    public Map<String, String> renovarToken(String refreshToken) {

        RefreshToken rt = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Token no encontrado"));

        if (rt.getExpiraEn().isBefore(LocalDateTime.now()))
            throw new RuntimeException("El token ha expirado");

        Usuario usuario = rt.getUsuario();
        String nuevoJwt = jwtService.generateToken(usuario.getEmail(), usuario.getTokenVersion());

        return Map.of("token", nuevoJwt);
    }

    @Transactional
    public void logout(String refreshToken) {

        RefreshToken rt = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Token no encontrado"));

        Usuario usuario = rt.getUsuario();
        // Al subir la versión, todos los JWT con versión anterior son rechazados
        // inmediatamente en la siguiente petición
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        usuarioRepository.save(usuario);

        refreshTokenRepository.delete(rt);
    }

    private void validarUsuario(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Ya existe un usuario con ese email");

        if (usuarioRepository.existsByDni(request.getDni()))
            throw new RuntimeException("Ya existe un usaurio con ese DNI");

    }

    private Usuario guardarUsuario(RegisterRequest request) {
        return usuarioRepository.save(Usuario.builder()
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .email(request.getEmail())
                .dni(request.getDni())
                .contrasenaHash(passwordEncoder.encode(request.getPassword()))
                .esActivo(true)
                .creadoEn(LocalDateTime.now())
                .build());
    }

    private void generarYEnviarCodigo(Usuario usuario) {

        codigo2faRepository.deleteByUsuario(usuario);

        String codigo = String.format("%06d", new Random().nextInt(1000000));

        codigo2faRepository.save(Codigo2fa.builder()
                .usuario(usuario)
                .codigo(codigo)
                .expiraEn(LocalDateTime.now().plusMinutes(10))
                .usado(false)
                .build());

        emailService.enviarCodigo(usuario.getEmail(), codigo);
    }

    @Transactional
    public void contrasenaOlvidada(ForgotPasswordRequest request) {

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No existe ninguna cuenta asociada a ese email"));

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

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsadoFalse(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token inválido o ya usado"));

        if (resetToken.getExpiraEn().isBefore(LocalDateTime.now()))
            throw new RuntimeException("El enlace ha expirado");

        Usuario usuario = resetToken.getUsuario();
        usuario.setContrasenaHash(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
