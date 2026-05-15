package com.jugamir.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "partida_categoria_pesos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartidaCategoriaWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private int peso;

}
