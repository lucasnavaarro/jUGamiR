package com.jugamir.backend.model;

import com.jugamir.backend.model.enums.ResultadoJugador;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "jugadores_partida")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JugadorPartida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Jugador jugador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @Column(name = "orden_union")
    private Integer ordenUnion; // nullable — orden en que se unió

    @Column(name = "orden_turno")
    private Integer ordenTurno; // nullable — se asigna al iniciar la partida

    @Column(nullable = false)
    private int puntos;

    @Column(name = "num_acertadas", nullable = false)
    private int numAcertadas;

    @Column(name = "num_falladas", nullable = false)
    private int numFalladas;

    @Column(name = "num_no_respondidas", nullable = false)
    private int numNoRespondidas;

    @Column(name = "tiempo_total", nullable = false)
    private int tiempoTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoJugador resultado;

    @Column(name = "unido_en", nullable = false)
    private OffsetDateTime unidoEn;

}
