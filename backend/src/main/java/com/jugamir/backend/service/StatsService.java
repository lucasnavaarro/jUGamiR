package com.jugamir.backend.service;

import com.jugamir.backend.dto.EstadisticasAsignaturasDTO;
import com.jugamir.backend.dto.EstadisticasCategoriaDTO;
import com.jugamir.backend.dto.EstadisticasGlobalesDTO;
import com.jugamir.backend.dto.PuntoProgresoDTO;
import com.jugamir.backend.dto.PuntoProgresoCategoriaDTO;
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
import java.util.Map;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

        private final JugadorPartidaRepository jugadorPartidaRepository;
        private final RespuestaJugadorRepository respuestaJugadorRepository;
        public static final double PCT = 100.0;

        public EstadisticasGlobalesDTO getEstadisticasGlobales(Long usuarioId) {

                // Obtiene todas las partidas del usuario filtrando las que aún no han
                // finalizado
                List<JugadorPartida> partidas = jugadorPartidaRepository
                                .findByJugador_Usuario_IdUsuario(usuarioId)
                                .stream()
                                .filter(jp -> jp.getResultado() != ResultadoJugador.PENDIENTE
                                                && !jp.getPartida().isModoEntrenamiento())
                                .toList();

                int partidasJugadas = partidas.size();
                int victorias = (int) partidas.stream()
                                .filter(jp -> jp.getResultado() == ResultadoJugador.VICTORIA)
                                .count();

                double porcentajeVictorias = partidasJugadas > 0
                                ? (victorias * PCT / partidasJugadas)
                                : 0.0;

                int totalAciertos = partidas.stream().mapToInt(JugadorPartida::getNumAcertadas).sum();
                int totalFallos = partidas.stream().mapToInt(JugadorPartida::getNumFalladas).sum();
                int totalNoRespondidas = partidas.stream().mapToInt(JugadorPartida::getNumNoRespondidas).sum();
                int totalPreguntas = totalAciertos + totalFallos + totalNoRespondidas;
                double porcentajeAcierto = totalPreguntas > 0
                                ? (totalAciertos * PCT / totalPreguntas)
                                : 0.0;

                List<RespuestaJugador> respuestas = respuestaJugadorRepository
                                .findByJugadorPartida_Jugador_Usuario_IdUsuario(usuarioId)
                                .stream()
                                .filter(rj -> !rj.getJugadorPartida().getPartida().isModoEntrenamiento())
                                .toList();
                double tiempoMedioMs = respuestas.stream()
                                .mapToInt(RespuestaJugador::getTiempoMs)
                                .average()
                                .orElse(0.0);

                List<EstadisticasCategoriaDTO> categorias = respuestas.stream()
                                .collect(Collectors.groupingBy(rj -> rj.getPreguntaPartida().getPregunta()
                                                .getAsignatura().getCategoria().getId()))
                                .entrySet().stream()
                                .map(entry -> {
                                        List<RespuestaJugador> rjList = entry.getValue(); // Extraemos todas las
                                                                                          // respuestas de la misma
                                                                                          // categoría
                                        var cat = rjList.get(0).getPreguntaPartida().getPregunta().getAsignatura()
                                                        .getCategoria();
                                        long total = rjList.size();
                                        long aciertos = rjList.stream().filter(RespuestaJugador::isEsCorrecta).count(); // Contamos
                                                                                                                        // las
                                                                                                                        // respuestas
                                                                                                                        // correctas
                                        long fallos = total - aciertos;
                                        double porcentaje = total > 0 ? (aciertos * PCT / total) : 0.0;
                                        double tiempoMedio = rjList.stream().mapToInt(RespuestaJugador::getTiempoMs)
                                                        .average().orElse(0.0);
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
                                .sorted(Comparator.comparingDouble(EstadisticasCategoriaDTO::porcentajeAcierto)
                                                .reversed()) // Ordenamos de mayor a menor porcentaje de aciertos
                                .toList();

                return new EstadisticasGlobalesDTO(
                                partidasJugadas,
                                victorias,
                                porcentajeVictorias,
                                totalAciertos + totalFallos,
                                totalAciertos,
                                totalFallos,
                                totalNoRespondidas,
                                porcentajeAcierto,
                                tiempoMedioMs,
                                categorias);

        }

        public List<EstadisticasAsignaturasDTO> getEstadisticasByCategoria(Long usuarioId, Long categoriaId) {

                return respuestaJugadorRepository
                                .findByJugadorPartida_Jugador_Usuario_IdUsuario(usuarioId)
                                .stream()
                                .filter(rj -> rj.getPreguntaPartida().getPregunta().getAsignatura().getCategoria()
                                                .getId()
                                                .equals(categoriaId)
                                                && !rj.getJugadorPartida().getPartida().isModoEntrenamiento())
                                .collect(Collectors.groupingBy(
                                                rj -> rj.getPreguntaPartida().getPregunta().getAsignatura().getId()))
                                .entrySet().stream()
                                .map(entry -> {
                                        List<RespuestaJugador> rjList = entry.getValue(); // Obtenemos todas las
                                                                                          // respuestas de la misma
                                                                                          // asignatura
                                        var asignatura = rjList.get(0).getPreguntaPartida().getPregunta()
                                                        .getAsignatura();
                                        long total = rjList.size();
                                        long aciertos = rjList.stream().filter(RespuestaJugador::isEsCorrecta).count(); // Contamos
                                                                                                                        // las
                                                                                                                        // respuestas
                                                                                                                        // correctas
                                        long fallos = total - aciertos;
                                        double porcentaje = total > 0 ? (aciertos * PCT / total) : 0.0;
                                        double tiempoMedio = rjList.stream().mapToInt(RespuestaJugador::getTiempoMs)
                                                        .average().orElse(0.0);

                                        return new EstadisticasAsignaturasDTO(
                                                        asignatura.getId(),
                                                        asignatura.getNombre(),
                                                        total,
                                                        aciertos,
                                                        fallos,
                                                        porcentaje,
                                                        tiempoMedio);
                                })
                                .sorted(Comparator.comparingDouble(EstadisticasAsignaturasDTO::porcentajeAcierto)
                                                .reversed()) // Ordenamos de mayor a menor porcentaje de aciertos
                                .toList();
        }

        public List<PuntoProgresoDTO> getProgresoGlobal(Long usuarioId) {

                List<JugadorPartida> partidas = jugadorPartidaRepository
                                .findByJugador_Usuario_IdUsuario(usuarioId)
                                .stream()
                                .filter(jp -> jp.getResultado() != ResultadoJugador.PENDIENTE
                                                && jp.getPartida().getTerminadaEn() != null
                                                && !jp.getPartida().isModoEntrenamiento())
                                .toList();

                if (partidas.isEmpty())
                        return List.of();

                LocalDate primera = partidas.stream()
                                .map(jp -> jp.getPartida().getTerminadaEn().toLocalDate())
                                .min(Comparator.naturalOrder()).get();

                LocalDate ultima = partidas.stream()
                                .map(jp -> jp.getPartida().getTerminadaEn().toLocalDate())
                                .max(Comparator.naturalOrder()).get();

                long dias = ChronoUnit.DAYS.between(primera, ultima);

                Function<JugadorPartida, String> clave;
                if (dias > 60) {
                        clave = jp -> jp.getPartida().getTerminadaEn().toLocalDate()
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM"));
                } else if (dias > 14) {
                        clave = jp -> jp.getPartida().getTerminadaEn().toLocalDate()
                                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
                } else {
                        clave = jp -> jp.getPartida().getTerminadaEn().toLocalDate().toString();
                }

                return partidas.stream()
                                .collect(Collectors.groupingBy(clave))
                                .entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(entry -> {
                                        List<JugadorPartida> grupo = entry.getValue();
                                        long totalGrupo = grupo.size();
                                        long victoriasGrupo = grupo.stream()
                                                        .filter(jp -> jp.getResultado() == ResultadoJugador.VICTORIA)
                                                        .count();
                                        double pctVictorias = totalGrupo > 0 ? (victoriasGrupo * PCT / totalGrupo)
                                                        : 0.0;
                                        double pctAcierto = grupo.stream().mapToDouble(jp -> {
                                                int total = jp.getNumAcertadas() + jp.getNumFalladas()
                                                                + jp.getNumNoRespondidas();
                                                return total > 0 ? (jp.getNumAcertadas() * PCT / total) : 0.0;
                                        }).average().orElse(0.0);
                                        double tiempoMedio = grupo.stream().mapToDouble(jp -> {
                                                int contestadas = jp.getNumAcertadas() + jp.getNumFalladas();
                                                return contestadas > 0 ? ((double) jp.getTiempoTotal() / contestadas)
                                                                : 0.0;
                                        }).average().orElse(0.0);
                                        return new PuntoProgresoDTO(entry.getKey(), pctVictorias, pctAcierto,
                                                        tiempoMedio);
                                })
                                .toList();
        }

        public List<PuntoProgresoCategoriaDTO> getProgresoPorCategoria(Long usuarioId, Long categoriaId) {
                List<RespuestaJugador> respuestas = respuestaJugadorRepository
                                .findByJugadorPartida_Jugador_Usuario_IdUsuario(usuarioId)
                                .stream()
                                .filter(rj -> rj.getPreguntaPartida().getPregunta()
                                                .getAsignatura().getCategoria().getId().equals(categoriaId)
                                                && rj.getJugadorPartida().getPartida().getTerminadaEn() != null
                                                && !rj.getJugadorPartida().getPartida().isModoEntrenamiento())
                                .toList();

                if (respuestas.isEmpty())
                        return List.of();

                LocalDate primera = respuestas.stream()
                                .map(rj -> rj.getJugadorPartida().getPartida().getTerminadaEn().toLocalDate())
                                .min(Comparator.naturalOrder()).get();
                LocalDate ultima = respuestas.stream()
                                .map(rj -> rj.getJugadorPartida().getPartida().getTerminadaEn().toLocalDate())
                                .max(Comparator.naturalOrder()).get();
                long dias = ChronoUnit.DAYS.between(primera, ultima);

                Function<RespuestaJugador, String> clave;
                if (dias > 60) {
                        clave = rj -> rj.getJugadorPartida().getPartida().getTerminadaEn().toLocalDate()
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM"));
                } else if (dias > 14) {
                        clave = rj -> rj.getJugadorPartida().getPartida().getTerminadaEn().toLocalDate()
                                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
                } else {
                        clave = rj -> rj.getJugadorPartida().getPartida().getTerminadaEn().toLocalDate()
                                        .toString();
                }

                return respuestas.stream()
                                .collect(Collectors.groupingBy(clave))
                                .entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(entry -> {
                                        List<RespuestaJugador> grupo = entry.getValue();
                                        long totalGrupo = grupo.size();
                                        long aciertosGrupo = grupo.stream().filter(RespuestaJugador::isEsCorrecta)
                                                        .count();
                                        double pctAcierto = totalGrupo > 0 ? (aciertosGrupo * PCT / totalGrupo) : 0.0;
                                        double tiempoMedio = grupo.stream().mapToInt(RespuestaJugador::getTiempoMs)
                                                        .average().orElse(0.0);
                                        return new PuntoProgresoCategoriaDTO(entry.getKey(), pctAcierto, tiempoMedio);
                                })
                                .toList();
        }

        public List<EstadisticasCategoriaDTO> getCategoriasOrdenadas(Long usuarioId, String criterio,
                        String direccion) {

                EstadisticasGlobalesDTO stats = getEstadisticasGlobales(usuarioId);

                // Extrae la lista de categorías y la copia en un ArrayList
                // (la copia es necesaria porque la lista original es inmutable)
                List<EstadisticasCategoriaDTO> categorias = new ArrayList<>(stats.categorias());

                Comparator<EstadisticasCategoriaDTO> comparator;
                if ("TIEMPO".equals(criterio) && "MEJORAR".equals(direccion)) {
                        comparator = Comparator.<EstadisticasCategoriaDTO>comparingDouble(c -> c.tiempoMedioMs())
                                        .reversed();

                } else if ("TIEMPO".equals(criterio)) {
                        comparator = Comparator.<EstadisticasCategoriaDTO>comparingDouble(c -> c.tiempoMedioMs());

                } else if ("ACIERTOS".equals(criterio) && "MEJORAR".equals(direccion)) {
                        comparator = Comparator.<EstadisticasCategoriaDTO>comparingDouble(c -> c.porcentajeAcierto());

                } else {
                        comparator = Comparator.<EstadisticasCategoriaDTO>comparingDouble(c -> c.porcentajeAcierto())
                                        .reversed();
                }

                return categorias.stream().sorted(comparator).toList();

        }

}
