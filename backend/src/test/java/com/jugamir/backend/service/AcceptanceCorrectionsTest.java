package com.jugamir.backend.service;

import com.jugamir.backend.dto.EditarPreguntaRequest;
import com.jugamir.backend.dto.RespuestaDTO;
import com.jugamir.backend.model.enums.Dificultad;
import com.jugamir.backend.model.enums.TipoPartida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Acceptance regressions from docs/BATERIA-PRUEBAS-jUGamiR.md.
 * Asserts the accepted behavior of the fixed regressions.
 * Never enabled against the application database: fixtures erase disposable data.
 */
@SpringBootTest(properties = {"app.deadlines.enabled=false", "spring.jpa.show-sql=false"})
@EnabledIfEnvironmentVariable(named = "JUGAMIR_DISPOSABLE_DB_TEST", matches = "true")
class AcceptanceCorrectionsTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired LobbyService lobbies;
    @Autowired JuegoService games;
    @Autowired ProfesorService professors;
    @Autowired StatsService stats;
    @MockitoBean EmailService mail;
    @MockitoBean org.springframework.messaging.simp.SimpMessagingTemplate messages;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired CommittedMessages committed;
    @Autowired com.jugamir.backend.repository.UsuarioRepository accounts;
    @Autowired com.jugamir.backend.repository.JugadorRepository players;
    @Autowired PerfilService profiles;
    Long host, guest, category, otherCategory, subject;

    @BeforeEach void fixtures() {
        assertEquals("audit_test", jdbc.queryForObject("select current_database()", String.class));
        jdbc.execute("TRUNCATE usuarios, categorias, security_rate_limits RESTART IDENTITY CASCADE");
        host = player("host");
        guest = player("guest");
        category = jdbc.queryForObject("insert into categorias(nombre,color) values ('QA A','#123456') returning id", Long.class);
        otherCategory = jdbc.queryForObject("insert into categorias(nombre,color) values ('QA B','#654321') returning id", Long.class);
        subject = jdbc.queryForObject("insert into asignaturas(nombre,categoria_id) values ('QA subject',?) returning id", Long.class, category);
    }

    Long player(String name) {
        Long id = jdbc.queryForObject("insert into usuarios(nombre,apellidos,email,contraseña_hash) values (?,'QA',?,'!disabled-test-password') returning id_usuario", Long.class, name, name + "@example.invalid");
        jdbc.update("insert into jugadores(id_usuario,nick) values (?,?)", id, name);
        return id;
    }

    Long create(int threshold, boolean training) {
        return lobbies.crearPartida(host, TipoPartida.PRIVADA, List.of(Dificultad.MEDIO), 30,
                training ? 1 : 3, List.of(category), threshold, training, null).getId();
    }

    Long question(String identifier) {
        Long id = jdbc.queryForObject("insert into preguntas(identificador,enunciado,estado,asignatura_id,creada_por) values (?,?,'PUBLICADA',?,?) returning id", Long.class, identifier, "Question " + identifier, subject, host);
        jdbc.update("insert into respuestas(texto_respuesta,es_correcta,orden,pregunta_id) values ('Correct',true,1,?),('Wrong',false,2,?)", id, id);
        return id;
    }

    void joinAndStart(Long game) {
        String code = jdbc.queryForObject("select codigo_union from partidas where id=?", String.class, game);
        lobbies.unirseAPartidaPrivada(code, guest);
        lobbies.iniciarPartida(game, host);
        jdbc.update("update partidas set turno_actual=(select orden_turno from jugadores_partida where partida_id=? and jugador_id=?) where id=?", game, host, game);
    }

    void answerCorrectly(Long game) {
        games.girarRuleta(game, host);
        Long instance = jdbc.queryForObject("select id from preguntas_partida where partida_id=? order by orden_pregunta desc limit 1", Long.class, game);
        Long answer = jdbc.queryForObject("select r.id from respuestas r join preguntas_partida pp on pp.pregunta_id=r.pregunta_id where pp.id=? and r.es_correcta", Long.class, instance);
        jdbc.update("update preguntas_partida set emitida_en=CURRENT_TIMESTAMP - INTERVAL '2 seconds',vence_en=CURRENT_TIMESTAMP + INTERVAL '20 seconds' where id=?", instance);
        games.responderPregunta(game, host, answer, instance);
    }


    @Test void rolledBackTransactionNeverPublishesItsEvents() {
        org.mockito.Mockito.clearInvocations(messages);
        var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
        assertThrows(IllegalStateException.class, () -> tx.execute(status -> {
            committed.send("/topic/test", Map.of("evento", "RESULTADO"));
            org.mockito.Mockito.verifyNoInteractions(messages);
            throw new IllegalStateException("Injected rollback");
        }));
        org.mockito.Mockito.verifyNoInteractions(messages);
        tx.execute(status -> { committed.send("/topic/test", Map.of("evento", "RESULTADO"));
            org.mockito.Mockito.verifyNoInteractions(messages); return null; });
        org.mockito.Mockito.verify(messages).convertAndSend("/topic/test", (Object) Map.of("evento", "RESULTADO"));
    }

    @Test void deferredCommitFailureDoesNotPublishAResult() {
        question("ROLLBACK"); Long game = create(3, true); lobbies.iniciarPartida(game, host);
        games.girarRuleta(game, host);
        Long instance = jdbc.queryForObject("select id from preguntas_partida where partida_id=?", Long.class, game);
        Long response = jdbc.queryForObject("select r.id from respuestas r join preguntas_partida p on p.pregunta_id=r.pregunta_id where p.id=? and r.es_correcta", Long.class, instance);
        jdbc.update("update preguntas_partida set emitida_en=now()-interval '1 second', vence_en=now()+interval '30 seconds' where id=?", instance);
        jdbc.execute("CREATE FUNCTION qa_commit_failure() RETURNS trigger LANGUAGE plpgsql AS $$BEGIN RAISE EXCEPTION 'injected failure'; END;$$");
        jdbc.execute("CREATE CONSTRAINT TRIGGER qa_commit_failure AFTER INSERT ON respuestas_jugador DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION qa_commit_failure()");
        org.mockito.Mockito.clearInvocations(messages);
        try {
            assertThrows(RuntimeException.class, () -> games.responderPregunta(game, host, response, instance));
            assertEquals(0, jdbc.queryForObject("select count(*) from respuestas_jugador", Integer.class));
            org.mockito.Mockito.verifyNoInteractions(messages);
        } finally {
            jdbc.execute("DROP TRIGGER qa_commit_failure ON respuestas_jugador");
            jdbc.execute("DROP FUNCTION qa_commit_failure()");
        }
    }

    @Test void usedQuestionRetirementPreservesHistoryAndCannotChangeSolution() {
        Long q = question("HISTORY"); Long game = create(3, true); lobbies.iniciarPartida(game, host);
        answerCorrectly(game);
        Long correct = jdbc.queryForObject("select id from respuestas where pregunta_id=? and es_correcta", Long.class, q);
        assertThrows(IllegalStateException.class, () -> professors.editarPregunta(q,
                new EditarPreguntaRequest("HISTORY", null, "Changed", null, null, false, Dificultad.MEDIO, subject, null)));
        assertDoesNotThrow(() -> professors.eliminarPregunta(q));
        assertEquals(1, jdbc.queryForObject("select count(*) from respuestas_jugador", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from preguntas_partida", Integer.class));
        assertEquals(correct, jdbc.queryForObject("select id from respuestas where pregunta_id=? and es_correcta", Long.class, q));
        assertTrue(jdbc.queryForObject("select anulada from preguntas where id=?", Boolean.class, q));
    }

    @Test void unusedQuestionCanBeDeletedWithoutDisablingDatabaseTriggers() throws Exception {
        Long q = question("UNUSED"); professors.eliminarPregunta(q);
        assertEquals(0, jdbc.queryForObject("select count(*) from preguntas where id=?", Integer.class, q));
        assertEquals(0, jdbc.queryForObject("select count(*) from respuestas where pregunta_id=?", Integer.class, q));
    }

    @Test void invalidOptionsAreRejectedBeforeChangingQuestionOrAnswers() {
        Long q = question("OPTIONS");
        var options = jdbc.queryForList("select id from respuestas where pregunta_id=? order by orden", Long.class, q);
        for (var values : List.of(
                List.of(new RespuestaDTO(options.get(0), "A", false, 1), new RespuestaDTO(options.get(1), "B", false, 2)),
                List.of(new RespuestaDTO(options.get(0), "A", true, 1), new RespuestaDTO(options.get(1), "B", true, 2)),
                List.of(new RespuestaDTO(options.get(0), "", true, 1)))) {
            assertThrows(IllegalArgumentException.class, () -> professors.editarPregunta(q,
                    new EditarPreguntaRequest("OPTIONS", null, "Changed", null, null, false, Dificultad.MEDIO, subject, values)));
        }
        assertEquals("Question OPTIONS", jdbc.queryForObject("select enunciado from preguntas where id=?", String.class, q));
    }

    @Test void normalizedIdentityQueriesAndUniqueIndexesAgree() {
        assertTrue(accounts.findByEmail("  HOST@EXAMPLE.INVALID  ").isPresent());
        assertTrue(players.existsByNick("  HOST  "));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update("insert into usuarios(nombre,apellidos,email,contraseña_hash) values ('QA','QA',' HOST@EXAMPLE.INVALID ','!disabled')"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update("update jugadores set nick=' HOST ' where id_usuario=?", guest));
    }

    @Test void singleCompetitivePlayerCannotStartButTrainingCan() {
        assertThrows(IllegalStateException.class, () -> lobbies.iniciarPartida(create(1, false), host));
        Long training = create(1, true); assertDoesNotThrow(() -> lobbies.iniciarPartida(training, host));
        assertThrows(IllegalArgumentException.class, () -> lobbies.crearPartida(host, TipoPartida.PUBLICA, List.of(Dificultad.MEDIO), 5, 1, List.of(category), 1, true, null));
        assertThrows(IllegalArgumentException.class, () -> lobbies.crearPartida(host, TipoPartida.PRIVADA, List.of(Dificultad.MEDIO), 600, 2, List.of(category), 1, true, null));
    }

    @Test void joinedPlayersAlwaysHaveTurnsWhenStartRacesWithJoin() throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            for (int attempt=0; attempt<5; attempt++) {
                Long game=create(1,false); String code=jdbc.queryForObject("select codigo_union from partidas where id=?",String.class,game);
                lobbies.unirseAPartidaPrivada(code, guest);
                Long late=player("late"+attempt);
                var gate=new java.util.concurrent.CyclicBarrier(2);
                var join=pool.submit(() -> { try { gate.await(); lobbies.unirseAPartidaPrivada(code,late); } catch (IllegalStateException expected) { } catch (Exception ex) { throw new RuntimeException(ex); } });
                var start=pool.submit(() -> { try { gate.await(); lobbies.iniciarPartida(game,host); } catch (Exception ex) { throw new RuntimeException(ex); } });
                join.get(10,java.util.concurrent.TimeUnit.SECONDS); start.get(10,java.util.concurrent.TimeUnit.SECONDS);
                assertEquals(0,jdbc.queryForObject("select count(*) from jugadores_partida where partida_id=? and resultado='PENDIENTE' and orden_turno is null",Integer.class,game));
            }
        } finally { pool.shutdownNow(); }
    }

    @Test void statisticsIgnoreAnswersUntilCompetitiveGameFinishes() {
        question("STATS"); Long game=create(3,false); joinAndStart(game); answerCorrectly(game);
        assertEquals(0,stats.getEstadisticasGlobales(host).partidasJugadas());
        assertTrue(stats.getEstadisticasGlobales(host).categorias().isEmpty());
        assertTrue(stats.getEstadisticasByCategoria(host,category).isEmpty());
        lobbies.abandonarPartida(game,guest);
        assertEquals(1,stats.getEstadisticasGlobales(host).partidasJugadas());
        assertEquals(1,stats.getEstadisticasGlobales(host).categorias().size());
        assertEquals(1,stats.getEstadisticasGlobales(guest).partidasJugadas());
    }

    @Test void deletionWithHistoryIsRejectedWithoutDeletingAccount() {
        create(1,false);
        assertThrows(com.jugamir.backend.exception.BusinessException.class,
                () -> profiles.eliminarCuenta(accounts.findById(host).orElseThrow()));
        assertTrue(accounts.existsById(host));
    }

    @Test void pageLimitsAreEnforcedAndReferencedSubjectsGiveBusinessConflict() {
        question("SUBJECT");
        assertThrows(IllegalStateException.class, () -> professors.eliminarAsignatura(subject));
        assertThrows(IllegalArgumentException.class, () -> professors.buscarPreguntas("",0,1000000));
    }
    @Test void cancelledGamesAreExcludedAndFinalAbandonmentsAndExpulsionsCountWithoutVictory() {
        Long game=create(1,false);
        jdbc.update("update partidas set estado='CANCELADA' where id=?",game);
        jdbc.update("update jugadores_partida set resultado='ABANDONADA' where partida_id=?",game);
        assertEquals(0,stats.getEstadisticasGlobales(host).partidasJugadas());
        jdbc.update("update partidas set estado='TERMINADA',terminada_en=now() where id=?",game);
        for(String result:List.of("ABANDONADA","EXPULSADO")) {
            jdbc.update("update jugadores_partida set resultado=cast(? as resultado_jugador) where partida_id=?",result,game);
            assertEquals(1,stats.getEstadisticasGlobales(host).partidasJugadas());
            assertEquals(0,stats.getEstadisticasGlobales(host).victorias());
        }
    }
    @Test void usedSubjectCannotMoveHistoricalAnswersToAnotherCategory() {
        question("HISTORICAL_CATEGORY");
        Long game = create(1, false);
        joinAndStart(game);
        answerCorrectly(game);
        var before = stats.getEstadisticasGlobales(host);
        assertTrue(before.categorias().stream().anyMatch(c -> c.categoriaId().equals(category)));
        assertThrows(IllegalStateException.class, () -> professors.editarAsignatura(subject,
                new com.jugamir.backend.dto.CrearAsignaturaRequest("QA renamed", otherCategory)));
        var after = stats.getEstadisticasGlobales(host);
        assertEquals(before.categorias(), after.categorias());
        assertDoesNotThrow(() -> professors.editarAsignatura(subject,
                new com.jugamir.backend.dto.CrearAsignaturaRequest("QA renamed", category)));
    }

}
