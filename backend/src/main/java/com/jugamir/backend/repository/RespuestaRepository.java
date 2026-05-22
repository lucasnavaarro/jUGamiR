package com.jugamir.backend.repository;

import com.jugamir.backend.model.Respuesta;
import com.jugamir.backend.model.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RespuestaRepository extends JpaRepository<Respuesta, Long> {

    // Obtiene las opciones de respuesta de una pregunta
    List<Respuesta> findByPregunta(Pregunta pregunta);

    void deleteByPregunta_Id(Long preguntaId);

    @Modifying
    @Query("UPDATE Respuesta r SET r.esCorrecta = false WHERE r.pregunta.id = :preguntaId")
    void setTodasIncorrectasByPregunta(@Param("preguntaId") Long preguntaId);

}
