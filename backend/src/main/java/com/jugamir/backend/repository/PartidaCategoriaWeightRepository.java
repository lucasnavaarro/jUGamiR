package com.jugamir.backend.repository;

import com.jugamir.backend.model.Partida;
import com.jugamir.backend.model.PartidaCategoriaWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PartidaCategoriaWeightRepository extends JpaRepository<PartidaCategoriaWeight, Long> {
    List<PartidaCategoriaWeight> findByPartida(Partida partida);

}
