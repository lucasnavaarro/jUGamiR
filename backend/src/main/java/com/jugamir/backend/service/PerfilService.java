package com.jugamir.backend.service;

import com.jugamir.backend.dto.EditarPerfilRequest;
import com.jugamir.backend.dto.PerfilResponseDTO;
import com.jugamir.backend.exception.BusinessException;
import com.jugamir.backend.model.Jugador;
import com.jugamir.backend.model.Profesor;
import com.jugamir.backend.model.Usuario;
import com.jugamir.backend.repository.JugadorRepository;
import com.jugamir.backend.repository.PreguntaRepository;
import com.jugamir.backend.repository.ProfesorRepository;
import com.jugamir.backend.repository.UsuarioRepository;
import com.jugamir.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final ProfesorRepository profesorRepository;
    private final PreguntaRepository preguntaRepository;
    private final EmailChangeService emailChangeService;
    private final com.jugamir.backend.security.AccountLock accounts;
    private final com.jugamir.backend.repository.PasswordResetTokenRepository resetTokens;
    private final com.jugamir.backend.repository.RefreshTokenRepository refreshTokens;
    private final com.jugamir.backend.repository.Codigo2faRepository loginCodes;

    public PerfilResponseDTO obtenerPerfil(Usuario usuario) {

        Optional<Jugador> jugador = jugadorRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        if (jugador.isPresent()) {
            return new PerfilResponseDTO(usuario.getNombre(), usuario.getApellidos(),
                    usuario.getEmail(), "JUGADOR", jugador.get().getNick(), null);
        }

        // Si llega aquí, no es jugador → es profesor, por eso no se usa Optional, ni
        // isPresent() y se usa findById()
        Profesor profesor = profesorRepository.findById(usuario.getIdUsuario()).orElseThrow();
        return new PerfilResponseDTO(usuario.getNombre(), usuario.getApellidos(),
                usuario.getEmail(), "PROFESOR", null, profesor.getDepartamento());
    }

    @Transactional
    public boolean editarPerfil(Usuario usuario, EditarPerfilRequest req) {
        usuario = accounts.current(usuario.getIdUsuario());

        // Validamos la unicidad del email
        if (!usuario.getEmail().equals(req.email()) && usuarioRepository.existsByEmail(req.email())) {
            throw new BusinessException("El email ya está registrado");
        }

        boolean emailCambiado = !usuario.getEmail().equals(req.email());

        if (emailCambiado) {
            emailChangeService.consume(usuario, req.email(), req.currentPassword(), req.emailCode());
            usuario.setTokenVersion(usuario.getTokenVersion() + 1);
            refreshTokens.deleteByUsuario(usuario);
            loginCodes.deleteByUsuario(usuario);
            resetTokens.deleteByUsuario(usuario);
        }

        usuario.setNombre(req.nombre());
        usuario.setApellidos(req.apellidos());
        usuario.setEmail(req.email());
        usuarioRepository.save(usuario);

        // Actualizar campo específico del rol
        Optional<Jugador> jugadorOpt = jugadorRepository.findByUsuario_IdUsuario(usuario.getIdUsuario());
        if (jugadorOpt.isPresent()) {
            Jugador jugador = jugadorOpt.get();
            if (!jugador.getNick().equals(req.nick()) &&
                    jugadorRepository.existsByNick(req.nick())) {
                throw new BusinessException("El nick ya está en uso");
            }
            jugador.setNick(req.nick());
            jugadorRepository.save(jugador);
        } else {
            Profesor profesor = profesorRepository.findById(usuario.getIdUsuario()).orElseThrow();
            profesor.setDepartamento(req.departamento());
            profesorRepository.save(profesor);
        }

        return emailCambiado;
    }

    @Transactional
    public void eliminarCuenta(Usuario usuario) {
        boolean esProfesor = profesorRepository.existsById(usuario.getIdUsuario());
        if (esProfesor && preguntaRepository.existsByCreadaPor(usuario)) {
            throw new BusinessException("No puedes eliminar tu cuenta mientras tengas preguntas creadas. Elimínalas primero desde el panel de gestión.");
        }
        usuarioRepository.delete(usuario);
    }
}
