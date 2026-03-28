package com.jugamir.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellidos;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String dni;

    @Column(name = "contraseña_hash", nullable = false)
    private String contrasenaHash;

    @Column(name = "es_activo", nullable = false)
    private Boolean esActivo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private Integer tokenVersion = 1;
}
