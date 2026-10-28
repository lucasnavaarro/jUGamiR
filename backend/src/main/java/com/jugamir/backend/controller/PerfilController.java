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
    private final com.jugamir.backend.service.EmailChangeService emailChanges;

    @GetMapping("/perfil")
    public ResponseEntity<PerfilResponseDTO> obtenerPerfil(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(perfilService.obtenerPerfil(usuario));
    }

    @PutMapping("/perfil")
    public ResponseEntity<?> editarPerfil(@AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody EditarPerfilRequest req) {
        boolean reauthenticate = perfilService.editarPerfil(usuario, req);
        return ResponseEntity.ok(Map.of("reauthenticate", reauthenticate));
    }

    @PostMapping("/email/codigo")
    public ResponseEntity<Void> emailCode(@AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody com.jugamir.backend.dto.EmailChangeRequest request) {
        emailChanges.issue(usuario.getIdUsuario(), request.email(), request.currentPassword());
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/perfil")
    public ResponseEntity<Void> eliminarCuenta(@AuthenticationPrincipal Usuario usuario) {
        perfilService.eliminarCuenta(usuario);
        return ResponseEntity.noContent().build();
    }
}
