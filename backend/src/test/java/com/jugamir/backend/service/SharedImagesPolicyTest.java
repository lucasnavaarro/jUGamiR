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
@SpringBootTest(properties = {"app.deadlines.enabled=false", "spring.jpa.show-sql=false", "imagenes.path=/tmp/jugamir-shared-images-unit"})
@EnabledIfEnvironmentVariable(named = "JUGAMIR_DISPOSABLE_DB_TEST", matches = "true")
class SharedImagesPolicyTest {
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


    byte[] png() throws Exception { var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray(); }
    org.springframework.mock.web.MockMultipartFile image() throws Exception {return new org.springframework.mock.web.MockMultipartFile("file","image.png","image/png",png());}
    @Test void IMG10_sharedImageCannotBeOverwrittenOrDeletedWhileReferenced() throws Exception {
        String name="imagenes/shared.png";professors.subirImagen(image(),name);Long a=question("IMAGE_A"),b=question("IMAGE_B");
        jdbc.update("update preguntas set imagen_url=? where id in (?,?)",name,a,b);
        assertThrows(IllegalStateException.class,()->professors.subirImagen(image(),name));
        assertThrows(IllegalStateException.class,()->professors.eliminarImagen("/"+name));
        assertTrue(java.nio.file.Files.exists(java.nio.file.Path.of("/tmp/jugamir-shared-images-unit",name)));
        assertEquals(2,jdbc.queryForObject("select count(*) from preguntas where imagen_url=?",Integer.class,name));
        assertDoesNotThrow(()->professors.subirImagen(image(),"imagenes/new-image.png"));
    }
    @Test void IMG10_zipChecksAllReferencesBeforePublishingAnyFile() throws Exception {
        Long a=question("ZIP_REFERENCE");jdbc.update("update preguntas set imagen_url='imagenes/protected.png' where id=?",a);
        var out=new java.io.ByteArrayOutputStream();try(var zip=new java.util.zip.ZipOutputStream(out)) {
            for(String name:List.of("imagenes/unpublished.png","imagenes/protected.png")){zip.putNextEntry(new java.util.zip.ZipEntry(name));zip.write(png());zip.closeEntry();}
        }
        var upload=new org.springframework.mock.web.MockMultipartFile("file","images.zip","application/zip",out.toByteArray());
        assertThrows(IllegalStateException.class,()->professors.descomprimirImagenes(upload));
        assertFalse(java.nio.file.Files.exists(java.nio.file.Path.of("/tmp/jugamir-shared-images-unit/imagenes/unpublished.png")));
    }
    @Test void IMG10_unreferencedAndMissingImagesMayBeDeleted() throws Exception {
        professors.subirImagen(image(),"imagenes/deletable.png");
        assertDoesNotThrow(()->professors.eliminarImagen("imagenes/deletable.png"));
        assertDoesNotThrow(()->professors.eliminarImagen("imagenes/not-present.png"));
    }
    @Test void IMG10_equivalentPathsCannotBypassReferenceProtection() throws Exception {
        String name = "imagenes/review/shared.png";
        professors.subirImagen(image(), name);
        Long q = question("PATH_ALIAS");
        jdbc.update("update preguntas set imagen_url=? where id=?", "https://qa.invalid/imagenes/review/shared.png", q);
        assertThrows(IllegalStateException.class, () -> professors.eliminarImagen("imagenes/review//shared.png"));
        assertThrows(IllegalStateException.class, () -> professors.subirImagen(image(), "imagenes/review//shared.png"));
        assertTrue(java.nio.file.Files.exists(java.nio.file.Path.of("/tmp/jugamir-shared-images-unit", name)));
    }

}
