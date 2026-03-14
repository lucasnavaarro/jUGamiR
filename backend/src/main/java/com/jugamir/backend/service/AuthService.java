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
    public AuthResponse verify(VerifyRequest request) {

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

        return new AuthResponse(jwtService.generateToken(usuario.getEmail()), usuario.getEmail(), "");
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
