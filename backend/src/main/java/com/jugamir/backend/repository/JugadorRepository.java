package com.jugamir.backend.repository;

import com.jugamir.backend.model.Jugador;
import com.jugamir.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {

    boolean existsByNick(String nick);

    boolean existsByUsuario(Usuario usuario);

}
