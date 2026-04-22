package com.jugamir.backend.controller;

import com.jugamir.backend.dto.GirarRuletaResponse;
import com.jugamir.backend.dto.ResponderPreguntaRequest;
import com.jugamir.backend.model.Categoria;
import com.jugamir.backend.model.Pregunta;
import com.jugamir.backend.model.Usuario;
import com.jugamir.backend.service.JuegoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/juego")
@RequiredArgsConstructor
public class JuegoController {

    private final JuegoService juegoService;

    @PostMapping("/{partidaId}/tirar")
    public ResponseEntity<GirarRuletaResponse> girarRuleta(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Long partidaId) {
        GirarRuletaResponse respuesta = juegoService.girarRuleta(partidaId, usuario.getIdUsuario());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{partidaId}/responder")
    public ResponseEntity<Void> responderPregunta(@AuthenticationPrincipal Usuario usuario,
            @PathVariable Long partidaId, @RequestBody ResponderPreguntaRequest request) {

        juegoService.responderPregunta(partidaId, usuario.getIdUsuario(), request.respuestaId(), request.tiempoMs());
        return ResponseEntity.ok().build();
    }
}
