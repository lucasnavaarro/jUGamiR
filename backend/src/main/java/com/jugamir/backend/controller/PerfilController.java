package com.jugamir.backend.controller;

import com.jugamir.backend.dto.EditarPerfilRequest;
import com.jugamir.backend.dto.PerfilResponseDTO;
import com.jugamir.backend.model.Usuario;
import com.jugamir.backend.service.PerfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuario")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService perfilService;

    @GetMapping("/perfil")
    public ResponseEntity<PerfilResponseDTO> obtenerPerfil(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(perfilService.obtenerPerfil(usuario));
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> editarPerfil(@AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody EditarPerfilRequest req) {
        String nuevoJwt = perfilService.editarPerfil(usuario, req);
        if (nuevoJwt != null) {
            return ResponseEntity.ok(Map.of("token", nuevoJwt));
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/perfil")
    public ResponseEntity<Void> eliminarCuenta(@AuthenticationPrincipal Usuario usuario) {
        perfilService.eliminarCuenta(usuario);
        return ResponseEntity.noContent().build();
    }
}
