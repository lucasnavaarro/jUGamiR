package com.jugamir.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "progreso_categoria")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgresoCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_partida_id", nullable = false)
    private JugadorPartida jugadorPartida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private int aciertos;
}
