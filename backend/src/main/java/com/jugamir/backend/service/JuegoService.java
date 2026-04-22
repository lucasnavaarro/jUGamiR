package com.jugamir.backend.service;

import com.jugamir.backend.dto.GirarRuletaResponse;
import com.jugamir.backend.model.*;
import com.jugamir.backend.model.enums.*;
import com.jugamir.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class JuegoService {

    private final PartidaRepository partidaRepository;
    private final JugadorPartidaRepository jugadorPartidaRepository;
    private final PreguntaRepository preguntaRepository;
    private final PreguntaPartidaRepository preguntaPartidaRepository;
    private final RespuestaRepository respuestaRepository;
    private final RespuestaJugadorRepository respuestaJugadorRepository;
    private final ProgresoCategoriaRepository progresoCategoriaRepository;
    private final QuesitosGanadosRepository quesitosGanadosRepository;
    private final CategoriaRepository categoriaRepository;

    public GirarRuletaResponse girarRuleta(Long partidaId, Long usuarioId) {

        Partida partida = getPartidaEnCurso(partidaId);
        validarTurno(partida, usuarioId);

        List<Categoria> categorias = categoriaRepository.findAll();
        if (categorias.isEmpty())
            throw new IllegalStateException("No hay categorías disponibles");

        Categoria categoria = categorias.get(new Random().nextInt(categorias.size()));

        // Obtener los Ids de las preguntas que ya han salido para no repetir
        List<Long> idsUsadas = preguntaPartidaRepository
                .findByPartidaOrderByOrdenPregunta(partida)
                .stream()
                .map(pp -> pp.getPregunta().getId())
                .toList();

        // Si todavia no hay preguntas usadas, pasamos -1L para que no filtre nada
        List<Long> filtro_usadas = idsUsadas.isEmpty() ? List.of(-1L) : idsUsadas;

        List<Pregunta> preguntas = preguntaRepository.findAleatoriasByCategoria(categoria.getId(),
                EstadoPregunta.PUBLICADA,
                filtro_usadas, 1);

        if (preguntas.isEmpty())
            throw new IllegalStateException("No hay preguntas disponibles para esta categoría");

        Pregunta pregunta = preguntas.get(0);

        // Registrar en PreguntasPartida
        PreguntaPartida preguntaPartida = PreguntaPartida.builder()
                .partida(partida)
                .pregunta(pregunta)
                .ordenPregunta(idsUsadas.size() + 1)
                .build();

        preguntaPartidaRepository.save(preguntaPartida);

        partida.setTotalPreguntas(partida.getTotalPreguntas() + 1);
        partidaRepository.save(partida);

        return new GirarRuletaResponse(categoria, pregunta);
    }

    public void responderPregunta(Long partidaId, Long usuarioId, Long respuestaId, int tiempoMs) {

        Partida partida = getPartidaEnCurso(partidaId);
        JugadorPartida jugador = validarTurno(partida, usuarioId);

        List<PreguntaPartida> lista = preguntaPartidaRepository.findByPartidaOrderByOrdenPregunta(partida);

        if (lista.isEmpty())
            throw new IllegalStateException("No hay preguntas en esta partida");

        PreguntaPartida preguntaPartida = lista.get(lista.size() - 1);

        Respuesta respuesta = respuestaRepository.findById(respuestaId)
                .orElseThrow(() -> new IllegalArgumentException("Respuesta no encontrada"));

        if (!respuesta.getPregunta().getId().equals(preguntaPartida.getPregunta().getId()))
            throw new IllegalStateException("La respuesta no pertenece a la pregunta");

        RespuestaJugador respuestaJugador = RespuestaJugador.builder()
                .jugadorPartida(jugador)
                .preguntaPartida(preguntaPartida)
                .respuesta(respuesta)
                .esCorrecta(respuesta.getEsCorrecta())
                .tiempoMs(tiempoMs)
                .respondidaEl(OffsetDateTime.now())
                .build();

        respuestaJugadorRepository.save(respuestaJugador);

        // Actualizar estadísticas del jugador
        if (respuesta.getEsCorrecta()) {

            jugador.setNumAcertadas(jugador.getNumAcertadas() + 1);
            Categoria categoria = preguntaPartida.getPregunta().getAsignatura().getCategoria();

            actualizarProgreso(jugador, categoria, partida);

        } else {
            jugador.setNumFalladas(jugador.getNumFalladas() + 1);
            if (partida.getEstado() == EstadoPartida.EN_CURSO) {
                avanzarTurno(partida);
            }
        }

        jugador.setTiempoTotal(jugador.getTiempoTotal() + tiempoMs);
        jugadorPartidaRepository.save(jugador);

    }

    private void actualizarProgreso(JugadorPartida jugador, Categoria categoria, Partida partida) {

        ProgresoCategoria progreso = progresoCategoriaRepository
                .findByJugadorPartidaAndCategoria_Id(jugador, categoria.getId())
                .orElseGet(() -> ProgresoCategoria.builder()
                        .jugadorPartida(jugador)
                        .categoria(categoria)
                        .aciertos(0)
                        .build());

        progreso.setAciertos(progreso.getAciertos() + 1);

        if (progreso.getAciertos() >= 5) {

            // Jugador gana quesito de esa categoria
            QuesitosGanados quesito = QuesitosGanados.builder()
                    .jugadorPartida(jugador)
                    .categoria(categoria)
                    .ganadoEl(OffsetDateTime.now())
                    .build();

            quesitosGanadosRepository.save(quesito);

            progreso.setAciertos(0);

            int quesitosJugador = quesitosGanadosRepository.countByJugadorPartida(jugador);
            long totalCategorias = categoriaRepository.count();

            if (quesitosJugador >= totalCategorias) {
                terminarPartida(partida, jugador);
                return;
            }
        }

        progresoCategoriaRepository.save(progreso);
    }

    private void terminarPartida(Partida partida, JugadorPartida ganador) {
        ganador.setResultado(ResultadoJugador.VICTORIA);
        jugadorPartidaRepository.save(ganador);

        jugadorPartidaRepository.findByPartida(partida).stream()
                .filter(jp -> jp.getResultado() == ResultadoJugador.PENDIENTE)
                .forEach(jp -> {
                    jp.setResultado(ResultadoJugador.DERROTA);
                    jugadorPartidaRepository.save(jp);
                });

        partida.setEstado(EstadoPartida.TERMINADA);
        partida.setTerminadaEn(OffsetDateTime.now());
        partidaRepository.save(partida);
    }

    private void avanzarTurno(Partida partida) {

        List<Integer> turnosActivos = jugadorPartidaRepository.findByPartida(partida)
                .stream()
                .filter(jp -> jp.getResultado() == ResultadoJugador.PENDIENTE)
                .map(JugadorPartida::getOrdenTurno) // Nos quedamos solo con el orden del turno
                .sorted() // Ordenamos los turnos de menor a mayor
                .toList();

        if (turnosActivos.isEmpty()) {
            return;
        }

        int turnoActual = partida.getTurnoActual();
        int siguienteTurno = turnosActivos.stream()
                .filter(t -> t > turnoActual) // Buscamos el primer turno mayor al actual
                .findFirst() // Nos quedamos con el primero
                .orElse(turnosActivos.get(0)); // Si no existe, nos quedamos con el 1

        partida.setTurnoActual(siguienteTurno);
        partidaRepository.save(partida);
    }

    private Partida getPartidaEnCurso(Long partidaId) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new IllegalArgumentException("Partida no encontrada"));

        if (partida.getEstado() != EstadoPartida.EN_CURSO)
            throw new IllegalStateException("La partida no está en curso");
        return partida;
    }

    private JugadorPartida validarTurno(Partida partida, Long usuarioId) {
        JugadorPartida jugador = jugadorPartidaRepository.findByPartidaAndJugador_Usuario_IdUsuario(partida, usuarioId)
                .orElseThrow(() -> new IllegalStateException("No estás jugando esta partida"));

        if (!jugador.getOrdenTurno().equals(partida.getTurnoActual()))
            throw new IllegalStateException("No es tu turno");
        return jugador;
    }
}
