package com.jugamir.backend.controller;

import com.jugamir.backend.dto.EstadisticasAsignaturasDTO;
import com.jugamir.backend.dto.EstadisticasCategoriaDTO;
import com.jugamir.backend.dto.EstadisticasGlobalesDTO;
import com.jugamir.backend.dto.PuntoProgresoDTO;
import com.jugamir.backend.dto.PuntoProgresoCategoriaDTO;
import com.jugamir.backend.model.Usuario;
import com.jugamir.backend.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/jugador")
    public ResponseEntity<EstadisticasGlobalesDTO> getEstadisticasGlobales(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(statsService.getEstadisticasGlobales(usuario.getIdUsuario()));
    }

    @GetMapping("/jugador/categoria/{categoriaId}")
    public ResponseEntity<List<EstadisticasAsignaturasDTO>> getEstadisticasByCategoria(
            @AuthenticationPrincipal Usuario usuario, @PathVariable Long categoriaId) {
        return ResponseEntity.ok(statsService.getEstadisticasByCategoria(usuario.getIdUsuario(), categoriaId));
    }

    @GetMapping("/jugador/progreso")
    public ResponseEntity<List<PuntoProgresoDTO>> getProgresoGlobal(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(statsService.getProgresoGlobal(usuario.getIdUsuario()));
    }

    @GetMapping("/jugador/progreso/categoria/{categoriaId}")
    public ResponseEntity<List<PuntoProgresoCategoriaDTO>> getProgresoPorCategoria(
            @AuthenticationPrincipal Usuario usuario, @PathVariable Long categoriaId) {
        return ResponseEntity.ok(statsService.getProgresoPorCategoria(usuario.getIdUsuario(), categoriaId));
    }

    @GetMapping("/jugador/categorias-entrenamiento")
    public ResponseEntity<List<EstadisticasCategoriaDTO>> getEstadisticasCategorias(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam String criterio,
            @RequestParam String direccion) {
        return ResponseEntity.ok(statsService.getCategoriasOrdenadas(usuario.getIdUsuario(), criterio, direccion));
    }
}
