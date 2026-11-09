package com.jugamir.backend.service;

import com.jugamir.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP requests to a random local port, with a disposable DB and mocked email. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.deadlines.enabled=false", "spring.jpa.show-sql=false"})
@EnabledIfEnvironmentVariable(named = "JUGAMIR_DISPOSABLE_DB_TEST", matches = "true")
class HttpFunctionalRegressionTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder passwords;
    @MockitoBean EmailService mail;
    final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    Long user, category;
    String playerToken, professorToken;
    static final String PASSWORD = "QA-password@123";

    @BeforeEach void fixtures() {
        assertEquals("audit_test", jdbc.queryForObject("select current_database()", String.class));
        jdbc.execute("TRUNCATE usuarios, categorias, security_rate_limits RESTART IDENTITY CASCADE");
        user = jdbc.queryForObject("insert into usuarios(nombre,apellidos,email,contraseña_hash) values ('QA','Player','qa@example.invalid',?) returning id_usuario", Long.class, passwords.encode(PASSWORD));
        jdbc.update("insert into jugadores(id_usuario,nick) values (?,'qa-player')", user);
        Long professor = jdbc.queryForObject("insert into usuarios(nombre,apellidos,email,contraseña_hash) values ('QA','Professor','qa@ugr.es',?) returning id_usuario", Long.class, passwords.encode(PASSWORD));
        jdbc.update("insert into profesores(id_usuario,departamento) values (?,'QA')", professor);
        category = jdbc.queryForObject("insert into categorias(nombre,color) values ('QA','#123456') returning id", Long.class);
        playerToken = jwt.generateToken("qa@example.invalid", 1, "JUGADOR");
        professorToken = jwt.generateToken("qa@ugr.es", 1, "PROFESOR");
    }

    HttpResponse<String> request(String method, String path, String json, String token, String cookie) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(10));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (cookie != null) builder.header("Cookie", cookie);
        if (json != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    String jsonField(String json, String field) {
        var matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertTrue(matcher.find(), "Missing JSON field " + field);
        return matcher.group(1);
    }

    String roomPayload(int players, int threshold, String difficulties) {
        return "{\"tipo\":\"PRIVADA\",\"dificultades\":" + difficulties + ",\"tiempoRespuesta\":30,\"maxJugadores\":" + players + ",\"categoriaIds\":[" + category + "],\"aciertosParaQuesito\":" + threshold + ",\"modoEntrenamiento\":false}";
    }

    @Test void API01_anonymousRequestsAreRejectedOnEveryProtectedFamily() throws Exception {
        for (String path : new String[]{"/api/auth/me", "/api/usuario/perfil", "/api/lobby/publicas", "/api/stats/jugador", "/api/profesor/asignaturas", "/api/juego/1/estado"}) {
            assertEquals(401, request("GET", path, null, null, null).statusCode(), path);
        }
    }

    @Test void API01_rolesAreEnforcedByHttpFilters() throws Exception {
        assertEquals(403, request("GET", "/api/profesor/asignaturas", null, playerToken, null).statusCode());
        assertEquals(403, request("GET", "/api/stats/jugador", null, professorToken, null).statusCode());
        assertEquals(403, request("GET", "/api/juego/1/estado", null, professorToken, null).statusCode());
        assertEquals(200, request("GET", "/api/usuario/perfil", null, professorToken, null).statusCode());
    }

    @Test void AUT01_SES01_loginVerifyRefreshAndLogoutWorkOverHttp() throws Exception {
        var login = request("POST", "/api/auth/login", "{\"email\":\"qa@example.invalid\",\"password\":\"" + PASSWORD + "\"}", null, null);
        assertEquals(202, login.statusCode());
        String challenge = jsonField(login.body(), "challengeId");
        String code = jdbc.queryForObject("select codigo from codigos_2fa where challenge_id=?", String.class, challenge);
        var verify = request("POST", "/api/auth/verify", "{\"challengeId\":\"" + challenge + "\",\"codigo\":\"" + code + "\"}", null, null);
        assertEquals(200, verify.statusCode());
        assertEquals("JUGADOR", jsonField(verify.body(), "rol"));
        assertFalse(verify.body().contains("refreshToken"));
        String cookie = verify.headers().firstValue("set-cookie").orElseThrow();
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Strict"));
        String cookiePair = cookie.split(";", 2)[0];
        var refresh = request("POST", "/api/auth/refresh", null, null, cookiePair);
        assertEquals(200, refresh.statusCode());
        String access = jsonField(refresh.body(), "token");
        assertEquals(200, request("GET", "/api/auth/me", null, access, null).statusCode());
        assertEquals(200, request("POST", "/api/auth/logout", null, null, cookiePair).statusCode());
        assertEquals(401, request("GET", "/api/auth/me", null, access, null).statusCode());
    }

    @Test void CRE01_privateRoomCanBeCreatedAndStartedThroughHttp() throws Exception {
        var create = request("POST", "/api/lobby/crear", roomPayload(2, 1, "[\"MEDIO\"]"), playerToken, null);
        assertEquals(200, create.statusCode(), create.body());
        Long game = jdbc.queryForObject("select id from partidas", Long.class);
        assertEquals(200, request("GET", "/api/lobby/" + game, null, playerToken, null).statusCode());
        assertEquals(409, request("POST", "/api/lobby/iniciar/" + game, null, playerToken, null).statusCode());
        Long guest = jdbc.queryForObject("insert into usuarios(nombre,apellidos,email,contraseña_hash) values ('Guest','QA','guest@example.invalid','!disabled') returning id_usuario", Long.class);
        jdbc.update("insert into jugadores(id_usuario,nick) values (?,'guest')", guest);
        String guestToken = jwt.generateToken("guest@example.invalid", 1, "JUGADOR");
        String code = jdbc.queryForObject("select codigo_union from partidas where id=?", String.class, game);
        assertEquals(200, request("POST", "/api/lobby/unirse/privada/" + code, null, guestToken, null).statusCode());
        assertEquals(200, request("POST", "/api/lobby/iniciar/" + game, null, playerToken, null).statusCode());
        assertEquals("EN_CURSO", jdbc.queryForObject("select estado::text from partidas where id=?", String.class, game));
    }

    @Test void SES10_disabledAccountMustNotUsePreviouslyIssuedJwtOverHttp() throws Exception {
        assertEquals(200, request("GET", "/api/auth/me", null, playerToken, null).statusCode());
        jdbc.update("update usuarios set es_activo=false where id_usuario=?", user);
        assertEquals(401, request("GET", "/api/auth/me", null, playerToken, null).statusCode(),
                "Disabled account still authenticates with an existing JWT");
    }

    @Test void SES10_disabledAccountMustNotRefreshExistingSession() throws Exception {
        jdbc.update("insert into refresh_tokens(token,usuario_id,expira_en) values ('qa-disabled-session',?,CURRENT_TIMESTAMP + INTERVAL '1 day')", user);
        jdbc.update("update usuarios set es_activo=false where id_usuario=?", user);
        assertEquals(401, request("POST", "/api/auth/refresh", null, null, "refreshToken=qa-disabled-session").statusCode());
    }

    @Test void CRE06_outOfRangeCapacityMustReturnValidationErrorRatherThan500() throws Exception {
        var response = request("POST", "/api/lobby/crear", roomPayload(7, 1, "[\"MEDIO\"]"), playerToken, null);
        assertEquals(400, response.statusCode(), "Invalid capacity must be validated before DB constraints: " + response.body());
        assertEquals(0, jdbc.queryForObject("select count(*) from partidas", Integer.class));
    }

    @Test void CRE06_zeroWedgeThresholdMustNotCreatePlayableRoom() throws Exception {
        var response = request("POST", "/api/lobby/crear", roomPayload(2, 0, "[\"MEDIO\"]"), playerToken, null);
        assertEquals(400, response.statusCode(), "Zero threshold must be rejected: " + response.body());
        assertEquals(0, jdbc.queryForObject("select count(*) from partidas", Integer.class));
    }

    @Test void CRE04_emptyDifficultiesMustBeRejectedAtHttpBoundary() throws Exception {
        var response = request("POST", "/api/lobby/crear", roomPayload(2, 1, "[]"), playerToken, null);
        assertEquals(400, response.statusCode(), "Empty difficulties must be rejected: " + response.body());
        assertEquals(0, jdbc.queryForObject("select count(*) from partidas", Integer.class));
    }

    @Test void CRE05_unknownCategoryAndENT06_foreignWeightsAreRejectedOverHttp() throws Exception {
        String unknownCategory = roomPayload(2, 1, "[\"MEDIO\"]")
                .replace("\"categoriaIds\":[" + category + "]", "\"categoriaIds\":[" + category + ",999999]");
        assertEquals(400, request("POST", "/api/lobby/crear", unknownCategory, playerToken, null).statusCode());
        Long otherCategory = jdbc.queryForObject("insert into categorias(nombre,color) values ('QA Other','#654321') returning id", Long.class);
        String foreignWeights = roomPayload(1, 1, "[\"MEDIO\"]")
                .replace("\"modoEntrenamiento\":false", "\"modoEntrenamiento\":true,\"categoriaPesos\":{\"" + otherCategory + "\":1}");
        assertEquals(400, request("POST", "/api/lobby/crear", foreignWeights, playerToken, null).statusCode());
        assertEquals(0, jdbc.queryForObject("select count(*) from partidas", Integer.class));
    }
}
