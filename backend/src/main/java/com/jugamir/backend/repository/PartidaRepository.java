package com.jugamir.backend.repository;

import com.jugamir.backend.model.Partida;
import com.jugamir.backend.model.enums.TipoPartida;
import com.jugamir.backend.model.enums.EstadoPartida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    // Para que el jugador se una con el código de la partida
    Optional<Partida> findByCodigoUnion(String codigoUnion);

    // Listar partidas públicas
    List<Partida> findByTipoAndEstado(TipoPartida tipo, EstadoPartida estado);

}
