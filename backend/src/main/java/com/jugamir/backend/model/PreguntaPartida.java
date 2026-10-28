package com.jugamir.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "preguntas_partida")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaPartida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    @Column(name = "emitida_en")
    private java.time.OffsetDateTime emitidaEn;
    @Column(name = "vence_en")
    private java.time.OffsetDateTime venceEn;
    @Column(name = "jugador_turno_id")
    private Long jugadorTurnoId;
    @Column(nullable = false)
    private boolean cerrada;

    @Column(name = "orden_pregunta", nullable = false)
    private Integer ordenPregunta;
}
