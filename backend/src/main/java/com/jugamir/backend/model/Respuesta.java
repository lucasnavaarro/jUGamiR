package com.jugamir.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "respuestas")
@Getter
@Setter
public class Respuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "texto_respuesta", nullable = false)
    private String textoRespuesta;

    @Column(name = "es_correcta", nullable = false)
    private Boolean esCorrecta;

    @Column(nullable = false)
    private int orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;
}
