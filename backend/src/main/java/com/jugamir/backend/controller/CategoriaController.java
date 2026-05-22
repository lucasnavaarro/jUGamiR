package com.jugamir.backend.controller;

import com.jugamir.backend.model.Categoria;
import com.jugamir.backend.model.Asignatura;
import com.jugamir.backend.repository.AsignaturaRepository;
import com.jugamir.backend.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;
    private final AsignaturaRepository asignaturaRepository;

    @GetMapping
    public ResponseEntity<List<Categoria>> getAllCategorias() {
        return ResponseEntity.ok(categoriaRepository.findAll());
    }

    @GetMapping("/con-asignaturas")
    public ResponseEntity<List<Map<String, Object>>> getCategoriasConAsignaturas() {
        List<Map<String, Object>> result = categoriaRepository.findAll().stream()
                .map(cat -> {
                    List<String> asigs = asignaturaRepository.findByCategoria_Id(cat.getId())
                            .stream().map(Asignatura::getNombre).toList();
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", cat.getId());
                    m.put("nombre", cat.getNombre());
                    m.put("color", cat.getColor());
                    m.put("asignaturas", asigs);
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

}
