package com.jugamir.backend.controller;

import com.jugamir.backend.dto.*;
import com.jugamir.backend.model.Usuario;
import com.jugamir.backend.service.ProfesorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profesor")
@RequiredArgsConstructor
public class ProfesorController {

    private final ProfesorService profesorService;

    @GetMapping("/asignaturas")
    public ResponseEntity<List<AsignaturaDTO>> listarAsignaturas() {
        return ResponseEntity.ok(profesorService.listarAsignaturas());
    }

    @PostMapping("/asignaturas")
    public ResponseEntity<AsignaturaDTO> crearAsignatura(@Valid @RequestBody CrearAsignaturaRequest request) {
        return ResponseEntity.ok(profesorService.crearAsignatura(request));
    }

    @PutMapping("/asignaturas/{id}")
    public ResponseEntity<AsignaturaDTO> editarAsignatura(@PathVariable Long id,
            @Valid @RequestBody CrearAsignaturaRequest request) {
        return ResponseEntity.ok(profesorService.editarAsignatura(id, request));
    }

    @DeleteMapping("/asignaturas/{id}")
    public ResponseEntity<Void> eliminarAsignatura(@PathVariable Long id) {
        profesorService.eliminarAsignatura(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/preguntas/importar")
    public ResponseEntity<ImportarResultadoDTO> importarPreguntas(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(profesorService.importarPreguntas(file, usuario.getIdUsuario()));
    }

    @GetMapping("/preguntas/buscar")
    public ResponseEntity<Map<String, Object>> buscarPreguntas(@RequestParam String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "35") int size) {
        return ResponseEntity.ok(profesorService.buscarPreguntas(q, page, size));
    }

    @PutMapping("/preguntas/{id}")
    public ResponseEntity<PreguntaResumenDTO> editarPregunta(
            @PathVariable Long id,
            @Valid @RequestBody EditarPreguntaRequest request) {
        return ResponseEntity.ok(profesorService.editarPregunta(id, request));
    }

    @DeleteMapping("/preguntas/{id}")
    public ResponseEntity<Void> eliminarPregunta(@PathVariable Long id) throws Exception {
        profesorService.eliminarPregunta(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/imagenes")
    public ResponseEntity<Map<String, String>> subirImagen(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ruta") String ruta) throws Exception {
        String url = profesorService.subirImagen(file, ruta);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/imagenes/zip")
    public ResponseEntity<Void> subirImagenesZip(
            @RequestParam("file") MultipartFile file) throws Exception {
        profesorService.descomprimirImagenes(file);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/imagenes")
    public ResponseEntity<Void> eliminarImagen(@RequestParam String url) throws Exception {
        profesorService.eliminarImagen(url);
        return ResponseEntity.noContent().build();
    }

}
