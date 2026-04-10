package com.jugamir.backend.repository;

import com.jugamir.backend.model.Partida;
import com.jugamir.backend.model.PreguntaPartida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PreguntaPartidaRepository extends JpaRepository<PreguntaPartida, Long> {

    // Obtiene las preguntas de una partida ordenadas por el orden de aparecieron
    List<PreguntaPartida> findByPartidaOrderByOrdenPregunta(Partida partidaId);
}
