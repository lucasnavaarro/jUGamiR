package com.jugamir.backend.repository;

import com.jugamir.backend.model.Jugador;
import com.jugamir.backend.model.Usuario;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {

    @org.springframework.data.jpa.repository.Query("select (count(j) > 0) from Jugador j where lower(trim(j.nick)) = lower(trim(:nick))")
    boolean existsByNick(@org.springframework.data.repository.query.Param("nick") String nick);

    boolean existsByUsuario(Usuario usuario);

    Optional<Jugador> findByUsuario_IdUsuario(Long usuarioId);

}
