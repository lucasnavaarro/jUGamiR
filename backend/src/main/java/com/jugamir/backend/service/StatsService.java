package com.jugamir.backend.service;

import com.jugamir.backend.dto.EstadisticasAsignaturasDTO;
import com.jugamir.backend.dto.EstadisticasCategoriaDTO;
import com.jugamir.backend.dto.EstadisticasGlobalesDTO;
import com.jugamir.backend.model.JugadorPartida;
import com.jugamir.backend.model.RespuestaJugador;
import com.jugamir.backend.model.enums.ResultadoJugador;
import com.jugamir.backend.repository.JugadorPartidaRepository;
import com.jugamir.backend.repository.RespuestaJugadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final JugadorPartidaRepository jugadorPartidaRepository;
    private final RespuestaJugadorRepository respuestaJugadorRepository;

    public EstadisticasGlobalesDTO getEstadisticasGlobales(Long usuarioId) {

        List<JugadorPartida> partidas = jugadorPartidaRepository
                .findByJugador_Usuario_IdUsuario(usuarioId)
                .stream()
                .filter(jp -> jp.getResultado() != ResultadoJugador.PENDIENTE)
                .toList();

        int partidasJugadas = partidas.size();
        int victorias = (int) partidas.stream()
                .filter(jp -> jp.getResultado() == ResultadoJugador.VICTORIA)
                .count();

        double porcentajeVictorias = partidasJugadas > 0
                ? (victorias * 100.0 / partidasJugadas)
                : 0.0;

        int totalAciertos = partidas.stream().mapToInt(JugadorPartida::getNumAcertadas).sum();
        int totalFallos = partidas.stream().mapToInt(JugadorPartida::getNumFalladas).sum();
        int totalNoRespondidas = partidas.stream().mapToInt(JugadorPartida::getNumNoRespondidas).sum();
        int totalPreguntas = totalAciertos + totalFallos + totalNoRespondidas;
        double porcentajeAcierto = totalPreguntas > 0
                ? (totalAciertos * 100.0 / totalPreguntas)
                : 0.0;

        List<RespuestaJugador> respuestas = respuestaJugadorRepository
                .findByJugadorPartida_Jugador_Usuario_IdUsuario(usuarioId);

        double tiempoMedioMs = respuestas.stream()
                .mapToInt(RespuestaJugador::getTiempoMs)
                .average()
                .orElse(0.0);

        List<EstadisticasCategoriaDTO> categorias = respuestas.stream()
                .collect(Collectors
                        .groupingBy(rj -> rj.getPreguntaPartida().getPregunta().getAsignatura().getCategoria().getId()))
                .entrySet().stream()
                .map(entry -> {
                    List<RespuestaJugador> rjList = entry.getValue();
                    var cat = rjList.get(0).getPreguntaPartida().getPregunta().getAsignatura().getCategoria();
                    long total = rjList.size();
                    long aciertos = rjList.stream().filter(RespuestaJugador::isEsCorrecta).count();
                    long fallos = total - aciertos;
                    double porcentaje = total > 0
                            ? (aciertos * 100.0 / total)
                            : 0.0;
                    double tiempoMedio = rjList.stream().mapToInt(RespuestaJugador::getTiempoMs).average().orElse(0.0);
                    return new EstadisticasCategoriaDTO(
                            cat.getId(),
                            cat.getNombre(),
                            cat.getColor(),
                            total,
                            aciertos,
                            fallos,
                            porcentaje,
                            tiempoMedio);
                })
                .sorted(Comparator.comparingDouble(EstadisticasCategoriaDTO::porcentajeAcierto).reversed())
                .toList();

        return new EstadisticasGlobalesDTO(
                partidasJugadas,
                victorias,
                porcentajeVictorias,
                totalAciertos,
                totalFallos,
                totalNoRespondidas,
                totalPreguntas,
                porcentajeAcierto,
                tiempoMedioMs,
                categorias);

    }

    public List<EstadisticasAsignaturasDTO> getEstadisticasByCategoria(Long usuarioId, Long categoriaId) {

        return respuestaJugadorRepository
                .findByJugadorPartida_Jugador_Usuario_IdUsuario(usuarioId)
                .stream()
                .filter(rj -> rj.getPreguntaPartida().getPregunta().getAsignatura().getCategoria().getId()
                        .equals(categoriaId))
                .collect(Collectors.groupingBy(rj -> rj.getPreguntaPartida().getPregunta().getAsignatura().getId()))
                .entrySet().stream()
                .map(entry -> {
                    List<RespuestaJugador> rjList = entry.getValue();
                    var asignatura = rjList.get(0).getPreguntaPartida().getPregunta().getAsignatura();
                    long total = rjList.size();
                    long aciertos = rjList.stream().filter(RespuestaJugador::isEsCorrecta).count();
                    long fallos = total - aciertos;
                    double porcentaje = total > 0 ? (aciertos * 100.0 / total) : 0.0;
                    double tiempoMedio = rjList.stream().mapToInt(RespuestaJugador::getTiempoMs).average().orElse(0.0);

                    return new EstadisticasAsignaturasDTO(
                            asignatura.getId(),
                            asignatura.getNombre(),
                            total,
                            aciertos,
                            fallos,
                            porcentaje,
                            tiempoMedio);
                })
                .sorted(Comparator.comparingDouble(EstadisticasAsignaturasDTO::porcentajeAcierto).reversed())
                .toList();
    }

}
