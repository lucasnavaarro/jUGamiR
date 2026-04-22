package com.jugamir.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "quesitos_ganados")
@IdClass(QuesitosGanadosId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuesitosGanados {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_partida_id", nullable = false)
    private JugadorPartida jugadorPartida;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "ganado_el", nullable = false)
    private OffsetDateTime ganadoEl;
}
