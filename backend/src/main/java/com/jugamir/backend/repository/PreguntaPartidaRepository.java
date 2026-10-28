package com.jugamir.backend.repository;

import com.jugamir.backend.model.Partida;
import com.jugamir.backend.model.PreguntaPartida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PreguntaPartidaRepository extends JpaRepository<PreguntaPartida, Long> {

    java.util.Optional<PreguntaPartida> findFirstByPartidaOrderByOrdenPreguntaDesc(Partida partida);

    @org.springframework.data.jpa.repository.Query("select distinct p.partida.id from PreguntaPartida p where p.cerrada = false and p.venceEn <= :now")
    List<Long> findExpiredGameIds(@org.springframework.data.repository.query.Param("now") java.time.OffsetDateTime now,
                                 org.springframework.data.domain.Pageable pageable);

    // Obtiene las preguntas de una partida ordenadas por el orden de aparecieron
    List<PreguntaPartida> findByPartidaOrderByOrdenPregunta(Partida partidaId);
}
