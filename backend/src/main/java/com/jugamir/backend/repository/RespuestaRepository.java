package com.jugamir.backend.repository;

import com.jugamir.backend.model.Respuesta;
import com.jugamir.backend.model.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RespuestaRepository extends JpaRepository<Respuesta, Long> {

    // Obtiene las opciones de respuesta de una pregunta
    List<Respuesta> findByPregunta(Pregunta pregunta);
}
