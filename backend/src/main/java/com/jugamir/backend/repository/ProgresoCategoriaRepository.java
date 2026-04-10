package com.jugamir.backend.repository;

import com.jugamir.backend.model.ProgresoCategoria;
import com.jugamir.backend.model.JugadorPartida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProgresoCategoriaRepository extends JpaRepository<ProgresoCategoria, Long> {

    // Obtiene todos los progresos de una partida
    List<ProgresoCategoria> findByJugadorPartida(JugadorPartida jugadorPartida);

    // Obtiene el progreso de una categoría concreta en una partida
    Optional<ProgresoCategoria> findByJugadorPartidaAndCategoria_Id(JugadorPartida jugadorPartida, Long categoria);

}
