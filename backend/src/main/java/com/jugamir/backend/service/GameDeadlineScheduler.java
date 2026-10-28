package com.jugamir.backend.service;
import com.jugamir.backend.repository.PreguntaPartidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.deadlines.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class GameDeadlineScheduler {
    private final PreguntaPartidaRepository questions;
    private final JuegoService games;
    @Scheduled(fixedDelay = 1000)
    public void expire() {
        for (Long id : questions.findExpiredGameIds(OffsetDateTime.now(), org.springframework.data.domain.PageRequest.of(0, 100))) {
            try { games.expirarPregunta(id); }
            catch (RuntimeException ex) {
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("No se pudo cerrar el turno de la partida {}", id);
            }
        }
    }
}
