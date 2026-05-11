package com.jugamir.backend.service;

import com.jugamir.backend.dto.GirarRuletaResponse;
import com.jugamir.backend.dto.PreguntaDTO;
import com.jugamir.backend.model.*;
import com.jugamir.backend.model.enums.*;
import com.jugamir.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;

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
        private final SimpMessagingTemplate messagingTemplate;

        public GirarRuletaResponse girarRuleta(Long partidaId, Long usuarioId) {

                Partida partida = getPartidaEnCurso(partidaId);
                validarTurno(partida, usuarioId);

                List<Categoria> categorias = partida.getCategorias();
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

                // TODO: quitar este bloque y descomentar el de abajo cuando no se necesite
                // probar imágenes
                // List<Pregunta> preguntasConImagen =
                // preguntaRepository.findAleatoriasConImagen(1);
                // if (preguntasConImagen.isEmpty())
                // throw new IllegalStateException("No hay preguntas con imagen disponibles");
                // Pregunta pregunta = preguntasConImagen.get(0);

                List<Pregunta> preguntas = preguntaRepository.findAleatoriasByCategoriaYDificultad(
                                categoria.getId(),
                                EstadoPregunta.PUBLICADA,
                                partida.getDificultad(),
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

                List<Map<String, Object>> opciones = respuestaRepository.findByPregunta(pregunta)
                                .stream()
                                .sorted((a, b) -> Integer.compare(a.getOrden(), b.getOrden()))
                                .map(r -> Map.<String, Object>of(
                                                "id", r.getId(),
                                                "texto", r.getTextoRespuesta(),
                                                "orden", r.getOrden()))
                                .toList();

                List<String> imagenesUrl = pregunta.getImagenUrl() != null && !pregunta.getImagenUrl().isBlank()
                                ? List.of(pregunta.getImagenUrl().split(" \\| "))
                                : List.of();

                Map<String, Object> preguntaMsg = new HashMap<>();
                preguntaMsg.put("id", pregunta.getId());
                preguntaMsg.put("enunciado", pregunta.getEnunciado());
                preguntaMsg.put("imagenesUrl", imagenesUrl);

                Map<String, Object> eventoMsg = new HashMap<>();
                eventoMsg.put("evento", "PREGUNTA");
                eventoMsg.put("categoria", Map.of("id", categoria.getId(), "nombre", categoria.getNombre(), "color",
                                categoria.getColor()));
                eventoMsg.put("pregunta", preguntaMsg);
                eventoMsg.put("respuestas", opciones);
                eventoMsg.put("turnoActual", partida.getTurnoActual());

                messagingTemplate.convertAndSend("/topic/juego/" + partidaId, (Object) eventoMsg);

                return new GirarRuletaResponse(categoria,
                                new PreguntaDTO(
                                                pregunta.getId(),
                                                pregunta.getIdentificador(),
                                                pregunta.getTituloIndice(),
                                                pregunta.getEnunciado(),
                                                pregunta.getImagenUrl(),
                                                pregunta.getAnio(),
                                                pregunta.getComentario(),
                                                pregunta.isAnulada(),
                                                pregunta.getDificultad(),
                                                pregunta.getEstado(),
                                                pregunta.getAsignatura().getId()),
                                opciones);
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
                Categoria categoriaObtenida = null;
                if (respuesta.getEsCorrecta()) {

                        jugador.setNumAcertadas(jugador.getNumAcertadas() + 1);
                        Categoria categoria = preguntaPartida.getPregunta().getAsignatura().getCategoria();

                        categoriaObtenida = actualizarProgreso(jugador, categoria, partida);

                } else {
                        jugador.setNumFalladas(jugador.getNumFalladas() + 1);
                        if (partida.getEstado() == EstadoPartida.EN_CURSO) {
                                avanzarTurno(partida);
                        }
                }

                jugador.setTiempoTotal(jugador.getTiempoTotal() + tiempoMs);
                jugadorPartidaRepository.save(jugador);

                // Mandar al forntend cuál es la respuesta correcta para que lo pueda mostrar
                Long respuestaCorrectaId = respuestaRepository.findByPregunta(preguntaPartida.getPregunta())
                                .stream()
                                .filter(Respuesta::getEsCorrecta)
                                .findFirst()
                                .map(Respuesta::getId)
                                .orElse(null);

                // Mandar al frontend los quesitos ganados y así mostrar los circulitos
                List<Map<String, Object>> quesitosActualizados = quesitosGanadosRepository.findByJugadorPartida(jugador)
                                .stream()
                                .map(q -> Map.<String, Object>of(
                                                "categoriaId", q.getCategoria().getId(),
                                                "color", q.getCategoria().getColor()))
                                .toList();

                // Mandar al frontend el progreso de los quesitos para poder actualizar los
                // números dentro del círculo
                List<Map<String, Object>> progresoActualizado = progresoCategoriaRepository
                                .findByJugadorPartida(jugador)
                                .stream()
                                .map(pc -> Map.<String, Object>of(
                                                "categoriaId", pc.getCategoria().getId(),
                                                "aciertos", pc.getAciertos()))
                                .toList();

                Map<String, Object> evento = new HashMap<>();
                evento.put("evento", "RESULTADO");
                evento.put("esCorrecta", respuesta.getEsCorrecta());
                evento.put("respuestaCorrectaId", respuestaCorrectaId);
                evento.put("respuestaElegidaId", respuestaId);
                evento.put("jugadorId", usuarioId);
                evento.put("turnoActual", partida.getTurnoActual());
                evento.put("estado", partida.getEstado().name());
                evento.put("tiempoMs", tiempoMs);
                evento.put("quesitos", quesitosActualizados);
                evento.put("progreso", progresoActualizado);
                if (categoriaObtenida != null) {
                        evento.put("nuevoQuesito", Map.of(
                                        "nombre", categoriaObtenida.getNombre(),
                                        "nick", jugador.getJugador().getNick()));
                }

                messagingTemplate.convertAndSend("/topic/juego/" + partidaId, (Object) evento);

        }

        @Transactional(readOnly = true)
        public Map<String, Object> obtenerEstadoJuego(Long partidaId) {

                Partida partida = getPartidaEnCurso(partidaId);

                List<Map<String, Object>> jugadores = jugadorPartidaRepository.findByPartida(partida)
                                .stream()
                                .filter(jp -> jp.getResultado() != ResultadoJugador.ABANDONADA
                                                && jp.getResultado() != ResultadoJugador.EXPULSADO)
                                .map(jp -> {
                                        List<Map<String, Object>> quesitos = quesitosGanadosRepository
                                                        .findByJugadorPartida(jp)
                                                        .stream()
                                                        .map(q -> Map.<String, Object>of(
                                                                        "categoriaId", q.getCategoria().getId(),
                                                                        "nombre", q.getCategoria().getNombre(),
                                                                        "color", q.getCategoria().getColor()))
                                                        .toList();

                                        List<Map<String, Object>> progreso = progresoCategoriaRepository
                                                        .findByJugadorPartida(jp)
                                                        .stream()
                                                        .map(pc -> Map.<String, Object>of(
                                                                        "categoriaId", pc.getCategoria().getId(),
                                                                        "aciertos", pc.getAciertos()))
                                                        .toList();

                                        return Map.<String, Object>of(
                                                        "idJugador", jp.getJugador().getIdUsuario(),
                                                        "nick", jp.getJugador().getNick(),
                                                        "ordenTurno", jp.getOrdenTurno(),
                                                        "quesitos", quesitos,
                                                        "progreso", progreso);
                                })
                                .toList();

                List<Map<String, Object>> categorias = partida.getCategorias()
                                .stream()
                                .map(c -> Map.<String, Object>of(
                                                "id", c.getId(),
                                                "nombre", c.getNombre(),
                                                "color", c.getColor()))
                                .toList();

                return Map.of(
                                "idPartida", partida.getId(),
                                "estado", partida.getEstado().name(),
                                "turnoActual", partida.getTurnoActual(),
                                "tiempoRespuesta", partida.getTiempoRespuesta(),
                                "aciertosParaQuesito", partida.getAciertosParaQuesito(),
                                "jugadores", jugadores,
                                "categorias", categorias);
        }

        public void pasarTurno(Long partidaId, Long usuarioId) {

                Partida partida = getPartidaEnCurso(partidaId);
                JugadorPartida jugador = validarTurno(partida, usuarioId);

                jugador.setNumNoRespondidas(jugador.getNumNoRespondidas() + 1);
                jugadorPartidaRepository.save(jugador);

                avanzarTurno(partida);

                messagingTemplate.convertAndSend(
                                "/topic/juego/" + partidaId,
                                (Object) Map.of(
                                                "evento", "TURNO_PASADO",
                                                "turnoActual", partida.getTurnoActual(),
                                                "jugadorId", usuarioId));

        }

        private Categoria actualizarProgreso(JugadorPartida jugador, Categoria categoria, Partida partida) {

                ProgresoCategoria progreso = progresoCategoriaRepository
                                .findByJugadorPartidaAndCategoria_Id(jugador, categoria.getId())
                                .orElseGet(() -> ProgresoCategoria.builder()
                                                .jugadorPartida(jugador)
                                                .categoria(categoria)
                                                .aciertos(0)
                                                .build());

                progreso.setAciertos(progreso.getAciertos() + 1);

                if (progreso.getAciertos() >= partida.getAciertosParaQuesito()) {

                        // Jugador gana quesito de esa categoria
                        QuesitosGanados quesito = QuesitosGanados.builder()
                                        .jugadorPartida(jugador)
                                        .categoria(categoria)
                                        .ganadoEl(OffsetDateTime.now())
                                        .build();

                        quesitosGanadosRepository.save(quesito);

                        progreso.setAciertos(0);
                        progresoCategoriaRepository.save(progreso);

                        int quesitosJugador = quesitosGanadosRepository.countByJugadorPartida(jugador);
                        long totalCategoriasPartida = partida.getCategorias().size();

                        if (quesitosJugador >= totalCategoriasPartida) {
                                terminarPartida(partida, jugador);
                        }

                        return categoria; // si gana quesito se devuelve la categoria
                }

                progresoCategoriaRepository.save(progreso);
                return null; // si no gana quesito no se devuelve nada
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
                JugadorPartida jugador = jugadorPartidaRepository
                                .findByPartidaAndJugador_Usuario_IdUsuario(partida, usuarioId)
                                .orElseThrow(() -> new IllegalStateException("No estás jugando esta partida"));

                if (!jugador.getOrdenTurno().equals(partida.getTurnoActual()))
                        throw new IllegalStateException("No es tu turno");
                return jugador;
        }
}
