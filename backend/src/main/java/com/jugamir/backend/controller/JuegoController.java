package com.jugamir.backend.controller;

import com.jugamir.backend.dto.GirarRuletaResponse;
import com.jugamir.backend.dto.ResponderPreguntaRequest;
import com.jugamir.backend.model.Usuario;
import com.jugamir.backend.service.JuegoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/juego")
@RequiredArgsConstructor
public class JuegoController {

    private final JuegoService juegoService;

    @PostMapping("/{partidaId}/girar")
    public ResponseEntity<GirarRuletaResponse> girarRuleta(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Long partidaId) {
        GirarRuletaResponse respuesta = juegoService.girarRuleta(partidaId, usuario.getIdUsuario());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{partidaId}/responder")
    public ResponseEntity<Void> responderPregunta(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Long partidaId, @Valid @RequestBody ResponderPreguntaRequest request) {

        juegoService.responderPregunta(partidaId, usuario.getIdUsuario(), request.respuestaId(), request.tiempoMs());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{partidaId}/estado")
    public ResponseEntity<Map<String, Object>> obtenerEstadoJuego(@PathVariable Long partidaId) {
        return ResponseEntity.ok(juegoService.obtenerEstadoJuego(partidaId));
    }

    @PostMapping("/{partidaId}/pasarse")
    public ResponseEntity<Void> pasarTurno(@AuthenticationPrincipal Usuario usuario, @PathVariable Long partidaId) {
        juegoService.pasarTurno(partidaId, usuario.getIdUsuario());
        return ResponseEntity.ok().build();
    }
}
