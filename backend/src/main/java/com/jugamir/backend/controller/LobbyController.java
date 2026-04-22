package com.jugamir.backend.controller;

import com.jugamir.backend.dto.CrearPartidaRequest;
import com.jugamir.backend.dto.PartidaPublicaDTO;
import com.jugamir.backend.model.Partida;
import com.jugamir.backend.model.Usuario;
import com.jugamir.backend.service.LobbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/lobby")
@RequiredArgsConstructor
public class LobbyController {

    private final LobbyService lobbyService;

    @PostMapping("/crear")
    public ResponseEntity<?> crear(@AuthenticationPrincipal Usuario usuario, @RequestBody CrearPartidaRequest request) {

        Partida partida = lobbyService.crearPartida(
                usuario.getIdUsuario(),
                request.tipo(),
                request.dificultad(),
                request.tiempoRespuesta(),
                request.maxJugadores());

        return ResponseEntity.ok(partida);

    }

    @GetMapping("publicas")
    public ResponseEntity<List<PartidaPublicaDTO>> listarPublicas() {
        return ResponseEntity.ok(lobbyService.obtenerPartidasPublicas());
    }

    @PostMapping("/unirse/publica/{partidaId}")
    public ResponseEntity<?> unirsePublica(@AuthenticationPrincipal Usuario usuario, @PathVariable Long partidaId) {

        lobbyService.unirseAPartidaPublica(partidaId, usuario.getIdUsuario());
        return ResponseEntity.ok().build();

    }

    @PostMapping("/unirse/privada/{codigo}")
    public ResponseEntity<?> unirsePrivada(@AuthenticationPrincipal Usuario usuario, @PathVariable String codigo) {

        lobbyService.unirseAPartidaPrivada(codigo, usuario.getIdUsuario());
        return ResponseEntity.ok().build();

    }

    @PostMapping("/iniciar/{partidaId}")
    public ResponseEntity<?> iniciar(@AuthenticationPrincipal Usuario usuario, @PathVariable Long partidaId) {

        lobbyService.iniciarPartida(partidaId, usuario.getIdUsuario());
        return ResponseEntity.ok().build();

    }

    @DeleteMapping("/abandonar/{partidaId}")
    public ResponseEntity<?> abandonar(@AuthenticationPrincipal Usuario usuario, @PathVariable Long partidaId) {

        lobbyService.abandonarPartida(partidaId, usuario.getIdUsuario());
        return ResponseEntity.ok().build();

    }

    @DeleteMapping("/expulsar/{partidaId}/{jugadorId}")
    public ResponseEntity<?> expulsar(@AuthenticationPrincipal Usuario usuario, @PathVariable Long partidaId,
            @PathVariable Long jugadorId) {

        lobbyService.expulsarJugador(partidaId, usuario.getIdUsuario(), jugadorId);
        return ResponseEntity.ok().build();

    }

}
