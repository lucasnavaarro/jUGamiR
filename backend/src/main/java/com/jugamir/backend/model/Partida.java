package com.jugamir.backend.model;

import com.jugamir.backend.model.enums.Dificultad;
import com.jugamir.backend.model.enums.EstadoPartida;
import com.jugamir.backend.model.enums.TipoPartida;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TipoPartida tipo; // PUBLICA | PRIVADA

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "partida_dificultades", joinColumns = @JoinColumn(name = "partida_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "dificultad", nullable = false)
    private List<Dificultad> dificultades = new ArrayList<>();

    @Column(name = "tiempo_respuesta", nullable = false)
    private int tiempoRespuesta;

    @Column(name = "aciertos_para_quesito", nullable = false)
    private int aciertosParaQuesito;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoPartida estado; // CREADA | RUNNING | TERMINADA | CANCELADA

    private Integer duracion; // nullable

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "partidas_categorias", joinColumns = @JoinColumn(name = "partida_id"), inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    private List<Categoria> categorias = new ArrayList<>();

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
