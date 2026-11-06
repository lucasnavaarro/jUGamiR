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
class FunctionalRegressionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired LobbyService lobbies;
    @Autowired JuegoService games;
    @Autowired ProfesorService professors;
    @Autowired StatsService stats;
    @MockitoBean EmailService mail;
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

    @Test void LOB15_expelledPlayerMustNeverReceiveATurnOnStart() {
        Long game = create(1, false);
        String code = jdbc.queryForObject("select codigo_union from partidas where id=?", String.class, game);
        lobbies.unirseAPartidaPrivada(code, guest);
        lobbies.expulsarJugador(game, host, guest);
        lobbies.unirseAPartidaPrivada(code, player("replacement"));
        lobbies.iniciarPartida(game, host);
        assertEquals(0, jdbc.queryForObject("select count(*) from jugadores_partida where partida_id=? and resultado='EXPULSADO' and orden_turno is not null", Integer.class, game),
                "Expelled participant was assigned a gameplay turn");
        assertNotNull(jdbc.queryForObject("select orden_turno from jugadores_partida where partida_id=? and jugador_id=?", Integer.class, game, host));
        assertDoesNotThrow(() -> games.obtenerEstadoJuego(game, host));
    }

    @Test void CRE04_emptyDifficultySelectionMustBeRejected() {
        assertThrows(IllegalArgumentException.class, () -> lobbies.crearPartida(host, TipoPartida.PRIVADA,
                List.of(), 30, 2, List.of(category), 1, false, null),
                "A room with no eligible difficulty must not be created");
        assertEquals(0, jdbc.queryForObject("select count(*) from partidas", Integer.class));
    }

    @Test void CRE05_unknownSelectedCategoryMustNotBeSilentlyIgnored() {
        assertThrows(IllegalArgumentException.class, () -> lobbies.crearPartida(host, TipoPartida.PRIVADA,
                List.of(Dificultad.MEDIO), 30, 2, List.of(category, 999999L), 1, false, null),
                "Unknown selected category must cause rejection rather than partial configuration");
        assertEquals(0, jdbc.queryForObject("select count(*) from partidas", Integer.class));
    }

    @Test void ENT06_trainingWeightsMustNotSelectAnUnselectedCategory() {
        assertThrows(IllegalArgumentException.class, () -> lobbies.crearPartida(host, TipoPartida.PRIVADA,
                List.of(Dificultad.MEDIO), 30, 1, List.of(category), 1, true, Map.of(otherCategory, 1)),
                "Training weights outside the selected categories must be rejected");
        assertEquals(0, jdbc.queryForObject("select count(*) from partidas", Integer.class));
    }

    @Test void PRO10_editingAQuestionMustNotModifyAnotherQuestionsOption() {
        Long target = question("QA_TARGET");
        Long other = question("QA_OTHER");
        Long foreignAnswer = jdbc.queryForObject("select id from respuestas where pregunta_id=? and not es_correcta", Long.class, other);
        var request = new EditarPreguntaRequest("QA_TARGET", null, "Question QA_TARGET", null,
                null, false, Dificultad.MEDIO, subject,
                List.of(new RespuestaDTO(foreignAnswer, "Unexpected foreign edit", false, 2)));
        assertThrows(IllegalArgumentException.class, () -> professors.editarPregunta(target, request));
        assertEquals("Wrong", jdbc.queryForObject("select texto_respuesta from respuestas where id=?", String.class, foreignAnswer),
                "Editing one question changed an option belonging to another question");
    }

    @Test void FIN01_thresholdSevenMustAllowSixCorrectAnswersBeforeAwardingWedge() {
        for (int i = 0; i < 7; i++) question("QA_THRESHOLD_" + i);
        Long game = create(7, false);
        joinAndStart(game);
        for (int i = 0; i < 6; i++) {
            int ordinal = i + 1;
            assertDoesNotThrow(() -> answerCorrectly(game), "Correct answer " + ordinal + " must be accepted for threshold seven");
        }
        assertEquals(6, jdbc.queryForObject("select aciertos from progreso_categoria", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from quesitos_ganados", Integer.class));
        answerCorrectly(game);
        assertEquals(1, jdbc.queryForObject("select count(*) from quesitos_ganados", Integer.class));
        assertEquals("TERMINADA", jdbc.queryForObject("select estado::text from partidas where id=?", String.class, game));
    }

    @Test void FIN01_maximumThresholdThirtyCanBeCompleted() {
        for (int i = 0; i < 30; i++) question("QA_MAX_" + i);
        Long game = create(30, false);
        joinAndStart(game);
        for (int i = 0; i < 29; i++) answerCorrectly(game);
        assertEquals(29, jdbc.queryForObject("select aciertos from progreso_categoria", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from quesitos_ganados", Integer.class));
        answerCorrectly(game);
        assertEquals(0, jdbc.queryForObject("select aciertos from progreso_categoria", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from quesitos_ganados", Integer.class));
        assertEquals("TERMINADA", jdbc.queryForObject("select estado::text from partidas where id=?", String.class, game));
    }

    @Test void CRE06_serviceRejectsCapacityAndThresholdOutsideSupportedBounds() {
        for (int capacity : new int[]{0, -1, 7, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> lobbies.crearPartida(host, TipoPartida.PRIVADA,
                    List.of(Dificultad.MEDIO), 30, capacity, List.of(category), 1, false, null));
        }
        for (int threshold : new int[]{0, -1, 31, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> create(threshold, false));
        }
        assertEquals(0, jdbc.queryForObject("select count(*) from partidas", Integer.class));
    }

    @Test void ENT06_weightsRejectNonpositiveAndOverflowButAcceptSelectedCategories() {
        for (int weight : new int[]{0, -1}) {
            assertThrows(IllegalArgumentException.class, () -> lobbies.crearPartida(host, TipoPartida.PRIVADA,
                    List.of(Dificultad.MEDIO), 30, 1, List.of(category), 1, true, Map.of(category, weight)));
        }
        assertThrows(IllegalArgumentException.class, () -> lobbies.crearPartida(host, TipoPartida.PRIVADA,
                List.of(Dificultad.MEDIO), 30, 1, List.of(category, otherCategory), 1, true,
                Map.of(category, Integer.MAX_VALUE, otherCategory, 1)));
        assertEquals(0, jdbc.queryForObject("select count(*) from partidas", Integer.class));
        Long game = lobbies.crearPartida(host, TipoPartida.PRIVADA,
                List.of(Dificultad.MEDIO), 30, 1, List.of(category), 1, true, Map.of(category, 3)).getId();
        assertEquals(3, jdbc.queryForObject("select peso from partida_categoria_pesos where partida_id=? and categoria_id=?", Integer.class, game, category));
    }

    @Test void PRO10_ownOptionsCanStillBeEditedAndCorrectAnswerChanged() {
        Long target = question("QA_EDIT");
        Long correct = jdbc.queryForObject("select id from respuestas where pregunta_id=? and es_correcta", Long.class, target);
        Long wrong = jdbc.queryForObject("select id from respuestas where pregunta_id=? and not es_correcta", Long.class, target);
        professors.editarPregunta(target, new EditarPreguntaRequest("QA_EDIT", null, "Edited question", null,
                null, false, Dificultad.MEDIO, subject, List.of(
                new RespuestaDTO(correct, "Now incorrect", false, 1),
                new RespuestaDTO(wrong, "Now correct", true, 2))));
        assertEquals("Edited question", jdbc.queryForObject("select enunciado from preguntas where id=?", String.class, target));
        assertEquals(wrong, jdbc.queryForObject("select id from respuestas where pregunta_id=? and es_correcta", Long.class, target));
        assertEquals("Now incorrect", jdbc.queryForObject("select texto_respuesta from respuestas where id=?", String.class, correct));
    }

    @Test void LOB15_expelledPlayerCannotPreventVictoryAfterActiveOpponentAbandons() {
        Long game = create(1, false);
        String code = jdbc.queryForObject("select codigo_union from partidas where id=?", String.class, game);
        Long activeOpponent = player("opponent");
        lobbies.unirseAPartidaPrivada(code, guest);
        lobbies.expulsarJugador(game, host, guest);
        lobbies.unirseAPartidaPrivada(code, activeOpponent);
        lobbies.iniciarPartida(game, host);
        lobbies.abandonarPartida(game, activeOpponent);
        assertEquals("TERMINADA", jdbc.queryForObject("select estado::text from partidas where id=?", String.class, game));
        assertEquals("VICTORIA", jdbc.queryForObject("select resultado::text from jugadores_partida where partida_id=? and jugador_id=?", String.class, game, host));
        assertEquals("EXPULSADO", jdbc.queryForObject("select resultado::text from jugadores_partida where partida_id=? and jugador_id=?", String.class, game, guest));
    }

    @Test void V5_upgradePreservesExistingProgressAndAllowsHigherThresholds() {
        Long game = create(7, false);
        Long membership = jdbc.queryForObject("select id from jugadores_partida where partida_id=?", Long.class, game);
        jdbc.update("insert into progreso_categoria(jugador_partida_id,categoria_id,aciertos) values (?,?,4)", membership, category);
        jdbc.execute("alter table progreso_categoria drop constraint progreso_categoria_aciertos_check");
        jdbc.execute("alter table progreso_categoria add constraint progreso_categoria_aciertos_check check (aciertos between 0 and 5)");
        try {
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> jdbc.update("update progreso_categoria set aciertos=6"));
        } finally {
            new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                    new org.springframework.core.io.ClassPathResource("db/migration/V5__expand_category_progress_limit.sql"))
                    .execute(java.util.Objects.requireNonNull(jdbc.getDataSource()));
        }
        assertEquals(4, jdbc.queryForObject("select aciertos from progreso_categoria", Integer.class));
        jdbc.update("update progreso_categoria set aciertos=29");
        assertEquals(29, jdbc.queryForObject("select aciertos from progreso_categoria", Integer.class));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("update progreso_categoria set aciertos=31"));
    }

    @Test void FIN04_singleCategoryVictoryPersistsAndCountsInCompetitiveStats() {
        question("QA_WIN");
        Long game = create(1, false);
        if (jdbc.queryForObject("select modo_entrenamiento from partidas where id=?", Boolean.class, game)) lobbies.iniciarPartida(game, host);
        else joinAndStart(game);
        answerCorrectly(game);
        assertEquals("TERMINADA", jdbc.queryForObject("select estado::text from partidas where id=?", String.class, game));
        assertEquals("VICTORIA", jdbc.queryForObject("select resultado::text from jugadores_partida where partida_id=? and jugador_id=?", String.class, game, host));
        assertEquals(1, jdbc.queryForObject("select count(*) from quesitos_ganados", Integer.class));
        assertEquals(1, stats.getEstadisticasGlobales(host).victorias());
    }

    @Test void ENT09_completedTrainingDoesNotChangeCompetitiveStats() {
        question("QA_TRAINING");
        Long game = create(1, true);
        if (jdbc.queryForObject("select modo_entrenamiento from partidas where id=?", Boolean.class, game)) lobbies.iniciarPartida(game, host);
        else joinAndStart(game);
        answerCorrectly(game);
        assertEquals("TERMINADA", jdbc.queryForObject("select estado::text from partidas where id=?", String.class, game));
        assertEquals(0, stats.getEstadisticasGlobales(host).partidasJugadas());
        assertTrue(stats.getEstadisticasGlobales(host).categorias().isEmpty());
        assertTrue(stats.getProgresoGlobal(host).isEmpty());
    }
}
