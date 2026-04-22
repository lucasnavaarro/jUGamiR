package com.jugamir.backend.model;

import com.jugamir.backend.model.enums.Dificultad;
import com.jugamir.backend.model.enums.EstadoPartida;
import com.jugamir.backend.model.enums.TipoPartida;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "partidas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_union", unique = true, nullable = false)
    private String codigoUnion;

    @Column(name = "max_jugadores", nullable = false)
    private int maxJugadores;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPartida tipo; // PUBLICA | PRIVADA

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Dificultad dificultad; // FÁCIL | MEDIO | DIFÍCIL

    @Column(name = "tiempo_respuesta", nullable = false)
    private int tiempoRespuesta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPartida estado; // CREADA | RUNNING | TERMINADA | CANCELADA

    private Integer duracion; // nullable

    @Column(name = "total_preguntas", nullable = false)
    private int totalPreguntas;

    @Column(name = "empezada_en")
    private OffsetDateTime empezadaEn; // nullable

    @Column(name = "terminada_en")
    private OffsetDateTime terminadaEn; // nullable

    @Column(name = "turno_actual", nullable = false)
    private Integer turnoActual;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creada_por") // nullable — ON DELETE SET NULL
    private Usuario creadaPor;

    @Column(name = "creada_en", nullable = false)
    private OffsetDateTime creadaEn;
}
