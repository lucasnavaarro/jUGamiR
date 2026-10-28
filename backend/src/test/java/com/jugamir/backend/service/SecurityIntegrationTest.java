package com.jugamir.backend.service;

import com.jugamir.backend.dto.*;
import com.jugamir.backend.model.*;
import com.jugamir.backend.repository.*;
import com.jugamir.backend.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"app.deadlines.enabled=false", "spring.jpa.show-sql=false"})
@EnabledIfEnvironmentVariable(named = "JUGAMIR_DISPOSABLE_DB_TEST", matches = "true")
class SecurityIntegrationTest {
    @Autowired AuthService auth;
    @Autowired LoginChallengeService challenges;
    @Autowired EmailChangeService emailChanges;
    @Autowired PerfilService profiles;
    @Autowired JuegoService games;
    @Autowired LobbyService lobbies;
    @Autowired GameAccess access;
    @Autowired AuthRateLimiter limiter;
    @Autowired UsuarioRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    @MockitoBean EmailService mail;
    Long userId;
    static final String EMAIL = "player@example.invalid";
    static final String PASSWORD = "Test-passphrase-123";

    @BeforeEach void fixtures() {
        // This class is explicitly enabled only for the disposable database runner.
        assertEquals("audit_test", jdbc.queryForObject("select current_database()", String.class));
        jdbc.execute("TRUNCATE usuarios, categorias, security_rate_limits RESTART IDENTITY CASCADE");
        userId = jdbc.queryForObject("INSERT INTO usuarios(nombre,apellidos,email,contraseña_hash) VALUES ('Test','Player',?,?) RETURNING id_usuario", Long.class, EMAIL, passwords.encode(PASSWORD));
        jdbc.update("INSERT INTO jugadores(id_usuario,nick) VALUES (?, 'test-player')", userId);
        reset(mail);
    }
    String login() { return auth.login(new LoginRequest(EMAIL, PASSWORD)); }
    String code(String challenge) { return jdbc.queryForObject("SELECT codigo FROM codigos_2fa WHERE challenge_id=?", String.class, challenge); }
    String resetLink() {
        String token = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO password_reset_tokens(token,id_usuario,expira_en) VALUES (?,?,CURRENT_TIMESTAMP + INTERVAL '10 minutes')", token, userId);
        return token;
    }
    RegisterProfesorRequest professorRequest(String email) {
        var request = new RegisterProfesorRequest();
        request.setNombre("Test"); request.setApellidos("Professor"); request.setEmail(email);
        request.setPassword("Test-password@123"); request.setDepartamento("Medicina");
        return request;
    }
    @Test void institutionalProfessorSignupNeedsNoAllowlistAndLoginStillRequiresEmailCode() {
        var first = professorRequest("first.professor@ugr.es");
        var second = professorRequest("SECOND.PROFESSOR@UGR.ES");
        auth.registerProfesor(first); auth.registerProfesor(second);
        assertEquals(2, jdbc.queryForObject("select count(*) from profesores", Integer.class));
        String challenge = auth.login(new LoginRequest(first.getEmail(), first.getPassword()));
        verify(mail).enviarCodigo(eq(first.getEmail()), anyString());
        assertEquals(0, jdbc.queryForObject("select count(*) from refresh_tokens", Integer.class));
        String correct = code(challenge), wrong = correct.equals("000000") ? "111111" : "000000";
        assertThrows(BadCredentialsException.class, () -> auth.verify(new VerifyRequest(challenge, wrong)));
        var session = auth.verify(new VerifyRequest(challenge, correct));
        assertEquals("PROFESOR", session.authResponse().rol());
        assertEquals("PROFESOR", jwt.getRolFromToken(session.authResponse().token()));
    }
    @Test void backendRejectsProfessorSignupOutsideTheExactInstitutionalDomain() {
        for (String email : new String[]{"teacher@gmail.com", "teacher@mail.ugr.es", "teacher@ugr.es.example.com"}) {
            assertThrows(com.jugamir.backend.exception.BusinessException.class,
                    () -> auth.registerProfesor(professorRequest(email)));
            assertFalse(users.existsByEmail(email));
        }
        assertEquals(0, jdbc.queryForObject("select count(*) from profesores", Integer.class));
    }
    @Test void professorCanChangeToAnyVerifiedInstitutionalMailboxButNotAnExternalOne() {
        var request = professorRequest("original.professor@ugr.es");
        auth.registerProfesor(request);
        var professor = users.findByEmail(request.getEmail()).orElseThrow();
        Long id = professor.getIdUsuario();
        assertThrows(com.jugamir.backend.exception.BusinessException.class,
                () -> emailChanges.issue(id, "external@gmail.com", request.getPassword()));
        String newEmail = "new.professor@ugr.es";
        emailChanges.issue(id, newEmail, request.getPassword());
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mail).enviarCodigoCambioEmail(eq(newEmail), captor.capture());
        assertThrows(com.jugamir.backend.exception.BusinessException.class,
                () -> profiles.editarPerfil(professor, new EditarPerfilRequest("Test", "Professor", newEmail,
                        null, "Medicina", request.getPassword(), null)));
        assertTrue(profiles.editarPerfil(professor, new EditarPerfilRequest("Test", "Professor", newEmail,
                null, "Medicina", request.getPassword(), captor.getValue())));
        assertEquals(newEmail, users.findById(id).orElseThrow().getEmail());
    }
    @Test void passwordRequiredAndWrongOtpAttemptsPersistAcrossRollbacks() {
        assertThrows(RuntimeException.class, () -> auth.login(new LoginRequest(EMAIL, "wrong")));
        assertEquals(0, jdbc.queryForObject("select count(*) from codigos_2fa", Integer.class));
        assertThrows(BadCredentialsException.class, () -> challenges.resend(EMAIL));
        String challenge=login(), correct=code(challenge);
        String wrong=correct.equals("000000") ? "111111" : "000000";
        for(int i=0;i<5;i++) assertThrows(BadCredentialsException.class, () -> auth.verify(new VerifyRequest(challenge,wrong)));
        assertEquals(5, jdbc.queryForObject("select attempts from codigos_2fa where challenge_id=?", Integer.class, challenge));
        assertThrows(BadCredentialsException.class, () -> auth.verify(new VerifyRequest(challenge,correct)));
        assertEquals(0, jdbc.queryForObject("select count(*) from refresh_tokens", Integer.class));
    }
    @Test void disabledAccountsCannotLoginOrUseAnAlreadyIssuedChallenge() {
        String challenge = login(), correct = code(challenge);
        jdbc.update("update usuarios set es_activo=false where id_usuario=?", userId);
        reset(mail);
        assertThrows(RuntimeException.class, () -> auth.login(new LoginRequest(EMAIL, PASSWORD)));
        assertThrows(BadCredentialsException.class, () -> auth.verify(new VerifyRequest(challenge, correct)));
        assertFalse(users.findById(userId).orElseThrow().getEsActivo());
        assertEquals(0, jdbc.queryForObject("select count(*) from refresh_tokens", Integer.class));
        verifyNoInteractions(mail);
    }
    @Test void resendDoesNotResetAttemptsOrExpiryAndIsRateLimited() {
        String challenge=login();
        jdbc.update("update codigos_2fa set attempts=3,last_sent=CURRENT_TIMESTAMP - INTERVAL '2 minutes' where challenge_id=?", challenge);
        Object expiry=jdbc.queryForObject("select expira_en from codigos_2fa", Object.class);
        challenges.resend(challenge);
        assertEquals(3, jdbc.queryForObject("select attempts from codigos_2fa", Integer.class));
        assertEquals(expiry, jdbc.queryForObject("select expira_en from codigos_2fa", Object.class));
        assertThrows(ResponseStatusException.class, () -> challenges.resend(challenge));
    }
    @Test void concurrentVerificationConsumesOtpOnlyOnce() throws Exception {
        String challenge=login(), code=code(challenge);
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        Callable<Boolean> attempt=() -> { start.await(); try { auth.verify(new VerifyRequest(challenge,code)); return true; } catch(RuntimeException ex) { return false; } };
        try {
            var one=pool.submit(attempt); var two=pool.submit(attempt); start.countDown();
            assertNotEquals(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        assertEquals(1, jdbc.queryForObject("select count(*) from refresh_tokens",Integer.class));
    }
    @Test void resetRevokesJwtRefreshAndPendingLoginAndLinkCannotBeReused() {
        String challenge=login(); var session=auth.verify(new VerifyRequest(challenge,code(challenge)));
        int oldVersion=users.findById(userId).orElseThrow().getTokenVersion();
        String pending=login(), pendingCode=code(pending), reset=resetLink();
        auth.resetearContrasena(new ResetPasswordRequest(reset,"New-test-password-456"));
        assertEquals(oldVersion+1,users.findById(userId).orElseThrow().getTokenVersion());
        assertThrows(RuntimeException.class, () -> auth.renovarToken(session.refreshToken()));
        assertThrows(RuntimeException.class, () -> auth.verify(new VerifyRequest(pending,pendingCode)));
        assertThrows(RuntimeException.class, () -> auth.resetearContrasena(new ResetPasswordRequest(reset,"Other-test-password")));
        assertThrows(RuntimeException.class, () -> auth.login(new LoginRequest(EMAIL,PASSWORD)));
    }
    @Test void concurrentResetCannotConsumeSameLinkTwice() throws Exception {
        String token=resetLink(); var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        Callable<Boolean> attempt=() -> { start.await(); try { auth.resetearContrasena(new ResetPasswordRequest(token,"New-test-password")); return true; } catch(RuntimeException ex) { return false; } };
        try { var one=pool.submit(attempt); var two=pool.submit(attempt); start.countDown(); assertNotEquals(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS)); }
        finally { pool.shutdownNow(); }
    }
    @Test void emailChangeRequiresPasswordAndVerifiedNewMailboxAndRevokesOldRecovery() {
        var user=users.findById(userId).orElseThrow(); String newEmail="new@example.invalid";
        String oldReset=resetLink();
        assertThrows(RuntimeException.class, () -> profiles.editarPerfil(user,new EditarPerfilRequest("Test","Player",newEmail,"test-player",null,null,null)));
        emailChanges.issue(userId,newEmail,PASSWORD);
        var captor=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mail).enviarCodigoCambioEmail(eq(newEmail),captor.capture());
        assertTrue(profiles.editarPerfil(user,new EditarPerfilRequest("Test","Player",newEmail,"test-player",null,PASSWORD,captor.getValue())));
        assertEquals(newEmail,users.findById(userId).orElseThrow().getEmail());
        assertThrows(RuntimeException.class, () -> auth.resetearContrasena(new ResetPasswordRequest(oldReset,"Attacker-password")));
        assertEquals(2,users.findById(userId).orElseThrow().getTokenVersion());
    }
    @Test void rateLimitIsPersistedAndAtomic() {
        limiter.check("test",EMAIL,2); limiter.check("test",EMAIL,2);
        assertThrows(ResponseStatusException.class, () -> limiter.check("test",EMAIL,2));
        assertThrows(ResponseStatusException.class, () -> limiter.check("test",EMAIL,2));
        assertEquals(4,jdbc.queryForObject("select hits from security_rate_limits",Integer.class));
    }
    Long game() {
        Long cat=jdbc.queryForObject("insert into categorias(nombre,color) values ('Test','#123456') returning id",Long.class);
        Long subject=jdbc.queryForObject("insert into asignaturas(nombre,categoria_id) values ('Test',?) returning id",Long.class,cat);
        Long question=jdbc.queryForObject("insert into preguntas(enunciado,comentario,estado,asignatura_id,creada_por) values ('Test?','secret solution','PUBLICADA',?,?) returning id",Long.class,subject,userId);
        jdbc.update("insert into respuestas(texto_respuesta,es_correcta,orden,pregunta_id) values ('A',true,1,?),('B',false,2,?)",question,question);
        Long game=jdbc.queryForObject("insert into partidas(codigo_union,estado,creada_por) values ('TESTCODE','EN_CURSO',?) returning id",Long.class,userId);
        jdbc.update("insert into jugadores_partida(partida_id,jugador_id,orden_turno) values (?,?,1)",game,userId);
        jdbc.update("insert into partidas_categorias values (?,?)",game,cat);
        jdbc.update("insert into partida_dificultades values (?,'MEDIO')",game);
        return game;
    }
    @Test void outsidersCannotReadPrivateLobbyOrGameAndExpelledMembersLoseAccess() {
        Long game=game();
        assertNotNull(games.obtenerEstadoJuego(game,userId));
        assertThrows(AccessDeniedException.class, () -> games.obtenerEstadoJuego(game,999L));
        assertThrows(AccessDeniedException.class, () -> lobbies.obtenerEstadoLobby(game,999L));
        jdbc.update("update jugadores_partida set resultado='EXPULSADO' where partida_id=?",game);
        assertThrows(AccessDeniedException.class, () -> access.requireMember(game,userId));
    }
    @Test void questionPayloadHidesSolutionAndServerClosesDeadlineExactlyOnce() {
        Long game=game(); var response=games.girarRuleta(game,userId);
        assertNull(response.pregunta().comentario());
        assertTrue(response.respuestas().stream().noneMatch(r -> r.containsKey("correcta") || r.containsKey("esCorrecta")));
        Long question=jdbc.queryForObject("select id from preguntas_partida",Long.class);
        Long answer=jdbc.queryForObject("select id from respuestas where es_correcta",Long.class);
        assertThrows(IllegalStateException.class, () -> games.girarRuleta(game,userId));
        assertThrows(IllegalStateException.class, () -> games.responderPregunta(game,userId,answer,question)); // animation has not ended
        assertThrows(IllegalStateException.class, () -> games.pasarTurno(game,userId,question+1));
        jdbc.update("update preguntas_partida set emitida_en=CURRENT_TIMESTAMP - INTERVAL '1 minute',vence_en=CURRENT_TIMESTAMP - INTERVAL '1 second'");
        assertThrows(IllegalStateException.class, () -> games.responderPregunta(game,userId,answer,question));
        games.expirarPregunta(game); games.expirarPregunta(game);
        assertTrue(jdbc.queryForObject("select cerrada from preguntas_partida",Boolean.class));
        assertEquals(1,jdbc.queryForObject("select num_no_respondidas from jugadores_partida",Integer.class));
        assertEquals(0,jdbc.queryForObject("select count(*) from respuestas_jugador",Integer.class));
    }
    @Test void acceptedAnswerUsesServerElapsedTimeAndCannotBeReplayed() {
        Long game=game(); games.girarRuleta(game,userId);
        Long question=jdbc.queryForObject("select id from preguntas_partida",Long.class);
        Long answer=jdbc.queryForObject("select id from respuestas where not es_correcta",Long.class);
        jdbc.update("update preguntas_partida set emitida_en=CURRENT_TIMESTAMP - INTERVAL '2 seconds',vence_en=CURRENT_TIMESTAMP + INTERVAL '20 seconds'");
        games.responderPregunta(game,userId,answer,question);
        int elapsed=jdbc.queryForObject("select tiempo_ms from respuestas_jugador",Integer.class);
        assertTrue(elapsed>=1900 && elapsed<10000);
        assertThrows(IllegalStateException.class, () -> games.responderPregunta(game,userId,answer,question));
        assertEquals(1,jdbc.queryForObject("select count(*) from respuestas_jugador",Integer.class));
    }
}
