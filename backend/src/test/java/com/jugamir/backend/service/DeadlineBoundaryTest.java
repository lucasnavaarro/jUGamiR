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
class DeadlineBoundaryTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired LobbyService lobbies;
    @Autowired JuegoService games;
    @Autowired ProfesorService professors;
    @Autowired StatsService stats;
    @MockitoBean EmailService mail;
    @MockitoBean java.time.Clock gameClock;
    @Autowired com.jugamir.backend.repository.PreguntaPartidaRepository pending;
    java.time.Instant instant = java.time.Instant.parse("2030-06-10T12:00:00Z");
    void now(java.time.Instant value) {
        instant=value;
        org.mockito.Mockito.when(gameClock.instant()).thenAnswer(call -> instant);
        org.mockito.Mockito.when(gameClock.getZone()).thenReturn(java.time.ZoneOffset.UTC);
    }

    Long host, guest, category, otherCategory, subject;

    @BeforeEach void fixtures() {
        now(instant);
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


    Long[] pendingQuestion() {
        question("CLOCK"); Long game=create(30,true);lobbies.iniciarPartida(game,host);games.girarRuleta(game,host);
        Long instance=jdbc.queryForObject("select id from preguntas_partida where partida_id=?",Long.class,game);
        Long answer=jdbc.queryForObject("select r.id from respuestas r join preguntas_partida p on p.pregunta_id=r.pregunta_id where p.id=? and r.es_correcta",Long.class,instance);
        return new Long[]{game,instance,answer};
    }
    java.time.Instant deadline(Long instance) {
        return jdbc.queryForObject("select vence_en from preguntas_partida where id=?",java.time.OffsetDateTime.class,instance).toInstant();
    }
    @Test void RES10_oneMicrosecondBeforeDeadlineAcceptsAndDoesNotExpireTwice() {
        var p=pendingQuestion();now(deadline(p[1]).minusNanos(1000));
        assertDoesNotThrow(()->games.responderPregunta(p[0],host,p[2],p[1]));now(deadline(p[1]));games.expirarPregunta(p[0]);
        assertEquals(1,jdbc.queryForObject("select count(*) from respuestas_jugador",Integer.class));
        assertEquals(0,jdbc.queryForObject("select num_no_respondidas from jugadores_partida where partida_id=?",Integer.class,p[0]));
    }
    @Test void RES10_exactDeadlineRejectsAnswerAndSchedulerClosesOnce() {
        var p=pendingQuestion();now(deadline(p[1]));
        assertThrows(IllegalStateException.class,()->games.responderPregunta(p[0],host,p[2],p[1]));
        var scheduler=new GameDeadlineScheduler(pending,games,gameClock);scheduler.expire();scheduler.expire();
        assertEquals(0,jdbc.queryForObject("select count(*) from respuestas_jugador",Integer.class));
        assertEquals(1,jdbc.queryForObject("select num_no_respondidas from jugadores_partida where partida_id=?",Integer.class,p[0]));
    }
    @Test void RES10_oneMicrosecondAfterDeadlineRejectsAnswer() {
        var p=pendingQuestion();now(deadline(p[1]).plusNanos(1000));
        assertThrows(IllegalStateException.class,()->games.responderPregunta(p[0],host,p[2],p[1]));
        games.expirarPregunta(p[0]);assertTrue(jdbc.queryForObject("select cerrada from preguntas_partida where id=?",Boolean.class,p[1]));
    }
    @Test void FIN10_abandonmentVersusExpiryBothLockTheSameGame() throws Exception {
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            for(int i=0;i<10;i++) {
                Long q=question("RACE"+i);Long game=create(30,false);joinAndStart(game);games.girarRuleta(game,host);
                Long instance=jdbc.queryForObject("select id from preguntas_partida where partida_id=?",Long.class,game);now(deadline(instance));
                var gate=new java.util.concurrent.CyclicBarrier(2);
                var leave=pool.submit(()->{try{gate.await();lobbies.abandonarPartida(game,host);}catch(Exception ex){throw new RuntimeException(ex);}});
                var expire=pool.submit(()->{try{gate.await();games.expirarPregunta(game);}catch(Exception ex){throw new RuntimeException(ex);}});
                leave.get(10,java.util.concurrent.TimeUnit.SECONDS);expire.get(10,java.util.concurrent.TimeUnit.SECONDS);games.expirarPregunta(game);
                assertEquals("TERMINADA",jdbc.queryForObject("select estado::text from partidas where id=?",String.class,game));
                assertTrue(jdbc.queryForObject("select cerrada from preguntas_partida where id=?",Boolean.class,instance));
                assertTrue(jdbc.queryForObject("select sum(num_no_respondidas) from jugadores_partida where partida_id=?",Integer.class,game)<=1);
            }
        } finally {pool.shutdownNow();}
    }

    void responseVersusAbandon(int threshold) throws Exception {
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);int answered=0,endedFirst=0;
        try {
            for(int i=0;i<10;i++) {
                question("ANSWER_RACE_"+threshold+"_"+i);Long game=create(threshold,false);joinAndStart(game);games.girarRuleta(game,host);
                Long instance=jdbc.queryForObject("select id from preguntas_partida where partida_id=?",Long.class,game);
                Long option=jdbc.queryForObject("select r.id from respuestas r join preguntas_partida p on p.pregunta_id=r.pregunta_id where p.id=? and r.es_correcta",Long.class,instance);
                now(deadline(instance).minusSeconds(10));var gate=new java.util.concurrent.CyclicBarrier(2);
                var reply=pool.submit(()->{try{gate.await();games.responderPregunta(game,host,option,instance);}catch(IllegalStateException expected){}catch(Exception ex){throw new RuntimeException(ex);}});
                var leave=pool.submit(()->{try{gate.await();lobbies.abandonarPartida(game,guest);}catch(IllegalStateException expected){}catch(Exception ex){throw new RuntimeException(ex);}});
                reply.get(10,java.util.concurrent.TimeUnit.SECONDS);leave.get(10,java.util.concurrent.TimeUnit.SECONDS);
                int count=jdbc.queryForObject("select count(*) from respuestas_jugador where pregunta_partida_id=?",Integer.class,instance);
                assertTrue(count<=1);if(count==1)answered++;else endedFirst++;
                assertEquals("TERMINADA",jdbc.queryForObject("select estado::text from partidas where id=?",String.class,game));
                assertEquals(1,jdbc.queryForObject("select count(*) from jugadores_partida where partida_id=? and resultado='VICTORIA'",Integer.class,game));
                assertEquals("VICTORIA",jdbc.queryForObject("select resultado::text from jugadores_partida where partida_id=? and jugador_id=?",String.class,game,host));
                assertEquals(count,jdbc.queryForObject("select num_acertadas from jugadores_partida where partida_id=? and jugador_id=?",Integer.class,game,host));
                now(deadline(instance));games.expirarPregunta(game);
                assertTrue(jdbc.queryForObject("select cerrada from preguntas_partida where id=?",Boolean.class,instance));
            }
        } finally {pool.shutdownNow();}
        System.out.println("FIN10 threshold="+threshold+" answered-before-abandon="+answered+" ended-before-answer="+endedFirst);
    }
    @Test void FIN10_abandonmentVersusNormalCorrectAnswer() throws Exception {responseVersusAbandon(30);}
    @Test void FIN10_abandonmentVersusLastWedge() throws Exception {responseVersusAbandon(1);}
}
