package com.jugamir.backend.repository;

import com.jugamir.backend.model.QuesitosGanados;
import com.jugamir.backend.model.QuesitosGanadosId;
import com.jugamir.backend.model.JugadorPartida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuesitosGanadosRepository extends JpaRepository<QuesitosGanados, QuesitosGanadosId> {

    // Obtiene que tiene un jugador
    List<QuesitosGanados> findByJugadorPartida(JugadorPartida jugadorPartida);

    // Cuenta los quesitos que tiene un jugador
    int countByJugadorPartida(JugadorPartida jugadorPartida);
}
