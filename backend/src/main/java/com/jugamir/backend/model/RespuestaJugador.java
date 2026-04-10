package com.jugamir.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "respuestas_jugador")
@Getter
@Setter
public class RespuestaJugador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_partida_id", nullable = false)
    private JugadorPartida jugadorPartida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_partida_id", nullable = false)
    private PreguntaPartida preguntaPartida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respuesta_id", nullable = false)
    private Respuesta respuesta;

    @Column(name = "es_correcta", nullable = false)
    private boolean esCorrecta;

    @Column(name = "tiempo_ms", nullable = false)
    private int tiempoMs;

    @Column(name = "respondida_el", nullable = false)
    private OffsetDateTime respondidaEl;
}
