package com.jugamir.backend.repository;

import com.jugamir.backend.model.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {

    boolean existsByNick(String nick);
}
