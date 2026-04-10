package com.jugamir.backend.model;

import com.jugamir.backend.model.enums.Dificultad;
import com.jugamir.backend.model.enums.EstadoPregunta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "preguntas")
@Getter
@Setter
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String identificador;

    @Column(name = "titulo_indice")
    private String tituloIndice;

    @Column(nullable = false)
    private String enunciado;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @Column(name = "anio")
    private Integer anio;

    @Column(name = "comentario")
    private String comentario;

    @Column(nullable = false)
    private boolean anulada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Dificultad dificultad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPregunta estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creada_por", nullable = false)
    private Usuario creadaPor;

    @Column(name = "creada_el", nullable = false)
    private OffsetDateTime creadaEl;

    @Column(name = "actualizada_el", nullable = false)
    private OffsetDateTime actualizadaEl;
}
