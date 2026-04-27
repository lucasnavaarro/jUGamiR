package com.jugamir.backend.service;

import com.jugamir.backend.dto.PartidaPublicaDTO;
import com.jugamir.backend.model.*;
import com.jugamir.backend.model.enums.*;
import com.jugamir.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Optional;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class LobbyService {

    private final PartidaRepository partidaRepository;
    private final JugadorRepository jugadorRepository;
    private final JugadorPartidaRepository jugadorPartidaRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Partida crearPartida(Long usuarioId, TipoPartida tipo, Dificultad dificultad, int tiempoRespuesta,
            int maxJugadores) {

        Jugador jugador = jugadorRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("El usuario no es un jugador"));

        String codigoUnion = (tipo == TipoPartida.PRIVADA) ? generarCodigoUnico() : null;

        Partida partida = Partida.builder()
                .codigoUnion(codigoUnion)
                .tipo(tipo)
                .dificultad(dificultad)
                .tiempoRespuesta(tiempoRespuesta)
                .maxJugadores(maxJugadores)
                .estado(EstadoPartida.ESPERANDO)
                .totalPreguntas(0)
                .creadaPor(jugador.getUsuario())
                .creadaEn(OffsetDateTime.now())
                .build();

        partida = partidaRepository.save(partida);
        if (tipo == TipoPartida.PUBLICA) {
            List<PartidaPublicaDTO> lista = obtenerPartidasPublicas();
            messagingTemplate.convertAndSend("/topic/partidas-publicas", (Object) lista);
        }

        JugadorPartida jugadorPartida = JugadorPartida.builder()
                .jugador(jugador)
                .partida(partida)
                .ordenUnion(1)
                .puntos(0)
                .numAcertadas(0)
                .numFalladas(0)
                .tiempoTotal(0)
                .resultado(ResultadoJugador.PENDIENTE)
                .unidoEn(OffsetDateTime.now())
                .build();

        jugadorPartidaRepository.save(jugadorPartida);

        return partida;
    }

    public void unirseAPartidaPrivada(String codigoUnion, Long usuarioId) {
        Partida partida = partidaRepository.findByCodigoUnion(codigoUnion)
                .orElseThrow(() -> new IllegalArgumentException("Código de partida incorrecto"));

        if (partida.getTipo() != TipoPartida.PRIVADA)
            throw new IllegalStateException("Esta partida no es privada");

        unirJugador(partida, usuarioId);
    }

    public void unirseAPartidaPublica(Long partidaId, Long usuarioId) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new IllegalArgumentException("Partida no encontrada"));

        if (partida.getTipo() != TipoPartida.PUBLICA)
            throw new IllegalStateException("Esta partida no es pública, necesitas un código de unión");

        unirJugador(partida, usuarioId);
    }

    @Transactional(readOnly = true)
    public List<PartidaPublicaDTO> obtenerPartidasPublicas() {
        return partidaRepository.findByTipoAndEstado(TipoPartida.PUBLICA.name(), EstadoPartida.ESPERANDO.name())
                .stream()
                .map(partida -> new PartidaPublicaDTO(
                        partida.getId(),
                        partida.getCreadaPor() != null ? partida.getCreadaPor().getNombre() : "Desconocido",
                        partida.getCreadaPor() != null ? partida.getCreadaPor().getEmail() : "",
                        jugadorPartidaRepository.countByPartidaAndResultado(partida, ResultadoJugador.PENDIENTE.name()),
                        partida.getMaxJugadores(),
                        partida.getDificultad().toString()))
                .toList();

    }

    public void iniciarPartida(Long partidaId, Long usuarioId) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new IllegalArgumentException("Partida no encontrada"));

        if (!partida.getCreadaPor().getIdUsuario().equals(usuarioId))
            throw new IllegalStateException("Solo el anfitrión puede iniciar la partida");

        if (!partida.getEstado().equals(EstadoPartida.ESPERANDO))
            throw new IllegalStateException("La partida no está en estado de espera");

        int numJugadores = jugadorPartidaRepository.countByPartida(partida);
        if (numJugadores < 2)
            throw new IllegalStateException("Se necesitan al menos 2 jugadores para iniciar la partida");

        // Asignar turnos aleatorios
        List<JugadorPartida> jugadores = jugadorPartidaRepository.findByPartida(partida);
        Collections.shuffle(jugadores);

        for (int i = 0; i < jugadores.size(); i++) {
            jugadores.get(i).setOrdenUnion(i + 1);
        }

        jugadorPartidaRepository.saveAll(jugadores);
        partida.setTurnoActual(1);
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setEmpezadaEn(OffsetDateTime.now());
        partidaRepository.save(partida);

        Map<String, Object> estadoLobby = construirEstadoLobby(partida);
        messagingTemplate.convertAndSend(
                "/topic/lobby/" + partida.getId(),
                (Object) estadoLobby);

        // Si es publica, actualizamos la lista de partidas publicas
        if (partida.getTipo() == TipoPartida.PUBLICA) {
            List<PartidaPublicaDTO> lista = obtenerPartidasPublicas();
            messagingTemplate.convertAndSend("/topic/partidas-publicas", (Object) lista);
        }

    }

    public void abandonarPartida(Long partidaId, Long usuarioId) {

        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new IllegalArgumentException("Partida no encontrada"));

        JugadorPartida jugadorPartida = jugadorPartidaRepository
                .findByPartidaAndJugador_Usuario_IdUsuario(partida, usuarioId)
                .orElseThrow(() -> new IllegalStateException("No estás unido a esta partida"));

        EstadoPartida estado = partida.getEstado();

        if (estado.equals(EstadoPartida.ESPERANDO)) {
            jugadorPartidaRepository.delete(jugadorPartida);

            // Si no quedan jugadores, se cancela la partida
            if (jugadorPartidaRepository.countByPartida(partida) == 0) {
                partida.setEstado(EstadoPartida.CANCELADA);
                partidaRepository.save(partida);
            }

            // Si es publica, actualizamos la lista de partidas publicas
            if (partida.getTipo() == TipoPartida.PUBLICA) {
                List<PartidaPublicaDTO> lista = obtenerPartidasPublicas();
                messagingTemplate.convertAndSend("/topic/partidas-publicas", (Object) lista);
            }

        } else if (estado.equals(EstadoPartida.EN_CURSO)) {
            jugadorPartida.setResultado(ResultadoJugador.ABANDONADA);
            jugadorPartidaRepository.save(jugadorPartida);

            long jugadoresActivos = jugadorPartidaRepository.findByPartida(partida)
                    .stream()
                    .filter(jp -> jp.getResultado() != ResultadoJugador.ABANDONADA) // filtra los que no tienen estado
                                                                                    // abandonada
                    .count();

            if (jugadoresActivos < 2) {
                jugadorPartidaRepository.findByPartida(partida).stream()
                        .filter(jp -> jp.getResultado() != ResultadoJugador.ABANDONADA)
                        .forEach(jp -> {
                            jp.setResultado(ResultadoJugador.VICTORIA);
                            jugadorPartidaRepository.save(jp);
                        });
                partida.setEstado(EstadoPartida.TERMINADA);
                partida.setTerminadaEn(OffsetDateTime.now());
                partidaRepository.save(partida);

            }

        } else {
            throw new IllegalStateException("No puedes abandonar una partida ya terminada o cancelada");
        }
    }

    public void expulsarJugador(Long partidaId, Long anfitrionId, Long expulsadoId) {

        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> new IllegalArgumentException("Partida no encontrada"));

        if (!partida.getCreadaPor().getIdUsuario().equals(anfitrionId))
            throw new IllegalStateException("Solo el anfitrión puede expulsar a un jugador");

        if (partida.getEstado() != EstadoPartida.ESPERANDO)
            throw new IllegalStateException("Solo puedes expulsar jugadores antes de empezar la partida");

        // El anfitrión no puede expulsarse a sí mismo
        if (anfitrionId.equals(expulsadoId))
            throw new IllegalStateException("No puedes expulsarte a ti mismo");

        JugadorPartida jugadorPartida = jugadorPartidaRepository
                .findByPartidaAndJugador_Usuario_IdUsuario(partida, expulsadoId)
                .orElseThrow(() -> new IllegalStateException("El jugador no está en esta partida"));

        if (!jugadorPartida.getResultado().equals(ResultadoJugador.PENDIENTE))
            throw new IllegalStateException("No puedes expulsar a un jugador que ya ha abandonado o ha sido expulsado");

        jugadorPartida.setResultado(ResultadoJugador.EXPULSADO);
        jugadorPartidaRepository.save(jugadorPartida);
    }

    private void unirJugador(Partida partida, Long usuarioId) {

        if (!partida.getEstado().equals(EstadoPartida.ESPERANDO))
            throw new IllegalStateException("La partida ya ha comenzado o ha terminado");

        int numJugadores = jugadorPartidaRepository.countByPartidaAndResultadoNot(partida,
                ResultadoJugador.EXPULSADO.name());
        if (numJugadores >= partida.getMaxJugadores())
            throw new IllegalStateException("La sala está llena");

        Jugador jugador = jugadorRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("El usuario no es un jugador"));

        // Evitar que el mismo jugador se una dos veces
        Optional<JugadorPartida> registroExistente = jugadorPartidaRepository
                .findByPartidaAndJugador_Usuario_IdUsuario(partida, usuarioId);
        if (registroExistente.isPresent()) {
            if (registroExistente.get().getResultado() == ResultadoJugador.EXPULSADO)
                throw new IllegalStateException("Has sido expulsado de esta partida");
            else
                throw new IllegalStateException("Ya estás unido a esta partida");
        }
        JugadorPartida jugadorPartida = JugadorPartida.builder()
                .jugador(jugador)
                .partida(partida)
                .ordenUnion(numJugadores + 1)
                .puntos(0)
                .numAcertadas(0)
                .numFalladas(0)
                .tiempoTotal(0)
                .resultado(ResultadoJugador.PENDIENTE)
                .unidoEn(OffsetDateTime.now())
                .build();

        jugadorPartidaRepository.save(jugadorPartida);

        // Si es publica, actualizamos el contador en la lista de partidas publicas
        if (partida.getTipo() == TipoPartida.PUBLICA) {
            List<PartidaPublicaDTO> lista = obtenerPartidasPublicas();
            messagingTemplate.convertAndSend("/topic/partidas-publicas", (Object) lista);
        }

        // Notificar a todos los jugadores del lobby que se ha unido uno nuevo
        Map<String, Object> estadoLobby = construirEstadoLobby(partida);
        messagingTemplate.convertAndSend(
                "/topic/lobby/" + partida.getId(),
                (Object) estadoLobby);
    }

    private Map<String, Object> construirEstadoLobby(Partida partida) {

        List<JugadorPartida> participantes = jugadorPartidaRepository.findByPartida(partida);
        List<Map<String, String>> jugadores = participantes.stream()
                .filter(jp -> jp.getResultado() == ResultadoJugador.PENDIENTE) // Descarta a los expulsados y
                                                                               // abandonados
                .map(jp -> Map.of("nick", jp.getJugador().getNick())) // Obtiene el nick del jugador
                .toList(); // Convierte la lista de jugadores a una lista de maps

        return Map.of(
                "idPartida", partida.getId(),
                "codigo", partida.getCodigoUnion() != null ? partida.getCodigoUnion() : "",
                "anfitrion", partida.getCreadaPor().getNombre(),
                "jugadores", jugadores,
                "maxJugadores", partida.getMaxJugadores(),
                "estado", partida.getEstado().name());

    }

    private String generarCodigoUnico() {
        Random random = new Random();
        String codigo;

        do {
            // Genera un código de 6 dígitos
            codigo = String.format("%06d", random.nextInt(1000000));
            // Comprueba si el código ya existe, si existe repite el proceso
        } while (partidaRepository.findByCodigoUnion(codigo).isPresent());

        return codigo;
    }
}
