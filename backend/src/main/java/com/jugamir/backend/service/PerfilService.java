package com.jugamir.backend.service;

import com.jugamir.backend.dto.EditarPerfilRequest;
import com.jugamir.backend.dto.PerfilResponseDTO;
import com.jugamir.backend.exception.BusinessException;
import com.jugamir.backend.model.Jugador;
import com.jugamir.backend.model.Profesor;
import com.jugamir.backend.model.Usuario;
import com.jugamir.backend.repository.JugadorRepository;
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
    private final JwtService jwtService;

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
    public String editarPerfil(Usuario usuario, EditarPerfilRequest req) {

        // Validamos la unicidad del email
        if (!usuario.getEmail().equals(req.email()) && usuarioRepository.existsByEmail(req.email())) {
            throw new BusinessException("El email ya está registrado");
        }

        boolean emailCambiado = !usuario.getEmail().equals(req.email());

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

        String rol = jugadorRepository.existsByUsuario(usuario) ? "JUGADOR" : "PROFESOR";

        // Solo generar nuevo JWT si cambió el email
        if (emailCambiado) {
            return jwtService.generateToken(req.email(), usuario.getTokenVersion(), rol);
        }
        return null;
    }
}
