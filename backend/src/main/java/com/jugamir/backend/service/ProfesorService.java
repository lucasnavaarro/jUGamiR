package com.jugamir.backend.service;

import com.jugamir.backend.dto.*;
import com.jugamir.backend.model.*;
import com.jugamir.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfesorService {

    private final AsignaturaRepository asignaturaRepository;
    private final CategoriaRepository categoriaRepository;
    private final PreguntaRepository preguntaRepository;
    private final RespuestaRepository respuestaRepository;
    private final jakarta.persistence.EntityManager entityManager;

    @Value("${scripts.path:db}")
    private String scriptsPath;

    @Value("${imagenes.path:src/main/resources/static}")
    private String imagenesPath;

    @Transactional(readOnly = true)
    public List<AsignaturaDTO> listarAsignaturas() {
        return asignaturaRepository.findAllByOrderByCategoria_NombreAscNombreAsc()
                .stream()
                .map(a -> new AsignaturaDTO(
                        a.getId(),
                        a.getNombre(),
                        a.getCategoria().getId(),
                        a.getCategoria().getNombre()))
                .toList();
    }

    public AsignaturaDTO crearAsignatura(CrearAsignaturaRequest request) {

        if (asignaturaRepository.existsByNombreIgnoreCase(request.nombre()))
            throw new IllegalArgumentException("Ya existe una asignatura con ese nombre");

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException("No existe esa categoría"));

        Asignatura asignatura = new Asignatura();
        asignatura.setNombre(request.nombre().trim());
        asignatura.setCategoria(categoria);

        asignaturaRepository.save(asignatura);

        return new AsignaturaDTO(
                asignatura.getId(),
                asignatura.getNombre(),
                categoria.getId(),
                categoria.getNombre());
    }

    public AsignaturaDTO editarAsignatura(Long id, CrearAsignaturaRequest request) {

        Asignatura asignatura = asignaturaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe esa asignatura"));

        if (!asignatura.getNombre().equalsIgnoreCase(request.nombre())
                && asignaturaRepository.existsByNombreIgnoreCase(request.nombre()))
            throw new IllegalArgumentException("Ya existe una asignatura con ese nombre");

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new IllegalArgumentException("No existe esa categoría"));

        asignatura.setNombre(request.nombre().trim());
        asignatura.setCategoria(categoria);

        asignaturaRepository.save(asignatura);

        return new AsignaturaDTO(
                asignatura.getId(),
                asignatura.getNombre(),
                categoria.getId(),
                categoria.getNombre());
    }

    public void eliminarAsignatura(Long id) {

        if (!asignaturaRepository.existsById(id))
            throw new IllegalArgumentException("No existe esa asignatura");

        asignaturaRepository.deleteById(id);
    }

    public ImportarResultadoDTO importarPreguntas(MultipartFile file, Long usuarioId) throws Exception {

        Path tempFile = Files.createTempFile("preguntas_import_", ".csv");
        file.transferTo(tempFile.toFile());

        try {
            String outputPreguntas = ejecutarScript("import_preguntas.py", tempFile.toString(), usuarioId);
            String outputRespuestas = ejecutarScript("import_respuestas.py", tempFile.toString(), usuarioId);

            int insertadas = extraerNumero(outputPreguntas, "Insertadas");
            int duplicadas = extraerNumero(outputPreguntas, "Duplicadas");
            int errores = extraerNumero(outputPreguntas, "Omitidas");

            List<String> detalle = new ArrayList<>();
            detalle.add(outputPreguntas.trim());
            detalle.add(outputRespuestas.trim());

            return new ImportarResultadoDTO(insertadas, duplicadas, errores, detalle);

        } finally {
            Files.deleteIfExists(tempFile);
        }

    }

    private String ejecutarScript(String scriptName, String csvPath, Long usuarioId) throws Exception {

        ProcessBuilder pb = new ProcessBuilder("python3", scriptsPath + "/" + scriptName, csvPath);
        pb.redirectErrorStream(true);
        pb.environment().put("CREADA_POR_ID", String.valueOf(usuarioId));
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        process.waitFor();
        return output;
    }

    private int extraerNumero(String output, String etiqueta) {
        for (String linea : output.split("\n")) {
            if (linea.contains(etiqueta)) {
                String[] partes = linea.split("[:\\s]+");
                for (String parte : partes) {
                    try {
                        return Integer.parseInt(parte.trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return 0;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buscarPreguntas(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Pregunta> resultado = preguntaRepository.buscarPorIdentificadorOEnunciado(q, pageable);
        List<PreguntaResumenDTO> dtos = resultado.getContent().stream()
                .map(this::mapToDTO)
                .toList();
        return Map.of("preguntas", dtos, "totalPaginas", resultado.getTotalPages(), "totalElementos",
                resultado.getTotalElements());
    }

    public PreguntaResumenDTO editarPregunta(Long id, EditarPreguntaRequest request) {

        Pregunta p = preguntaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada"));

        // Verificar identificador duplicado (ignorando la pregunta actual)
        if (!p.getIdentificador().equals(request.identificador()) &&
                preguntaRepository.existsByIdentificador(request.identificador()))
            throw new IllegalArgumentException("Ya existe una pregunta con ese identificador");

        // Verificar enunciado duplicado (ignorando la pregunta actual)
        if (!p.getEnunciado().equals(request.enunciado()) &&
                preguntaRepository.existsByEnunciado(request.enunciado()))
            throw new IllegalArgumentException("Ya existe una pregunta con ese enunciado");

        Asignatura asignatura = asignaturaRepository.findById(request.asignaturaId())
                .orElseThrow(() -> new IllegalArgumentException("Asignatura no encontrada"));

        p.setIdentificador(request.identificador());
        p.setTituloIndice(request.tituloIndice());
        p.setEnunciado(request.enunciado());
        p.setImagenUrl(request.imagenUrl());
        p.setComentario(request.comentario());
        p.setAnulada(request.anulada());
        p.setDificultad(request.dificultad());
        p.setAsignatura(asignatura);
        p.setActualizadaEl(OffsetDateTime.now());
        preguntaRepository.save(p);

        if (request.respuestas() != null) {
            // Le dice a PostgreSQL que posponga la validación del constraint
            // uq_una_correcta_por_pregunta hasta el final de la transacción (el COMMIT).
            entityManager.createNativeQuery("SET CONSTRAINTS uq_una_correcta_por_pregunta DEFERRED").executeUpdate();
            // Primero actualizamos la correcta para no violar el trigger
            for (RespuestaDTO dto : request.respuestas()) {
                if (dto.esCorrecta()) {
                    respuestaRepository.findById(dto.id()).ifPresent(r -> {
                        r.setTextoRespuesta(dto.texto());
                        r.setEsCorrecta(true);
                        respuestaRepository.save(r);
                    });
                }
            }
            // Luego actualizamos las incorrectas
            for (RespuestaDTO dto : request.respuestas()) {
                if (!dto.esCorrecta()) {
                    respuestaRepository.findById(dto.id()).ifPresent(r -> {
                        r.setTextoRespuesta(dto.texto());
                        r.setEsCorrecta(false);
                        respuestaRepository.save(r);
                    });
                }
            }
        }
        return mapToDTO(p);
    }

    private PreguntaResumenDTO mapToDTO(Pregunta p) {
        List<RespuestaDTO> respuestas = respuestaRepository.findByPregunta(p)
                .stream()
                .sorted((a, b) -> Integer.compare(a.getOrden(), b.getOrden()))
                .map(r -> new RespuestaDTO(r.getId(), r.getTextoRespuesta(), r.getEsCorrecta(), r.getOrden()))
                .toList();
        return new PreguntaResumenDTO(p.getId(), p.getIdentificador(), p.getTituloIndice(),
                p.getEnunciado(), p.getImagenUrl(), p.getAnio(),
                p.getComentario(), p.isAnulada(), p.getDificultad(),
                p.getEstado(), p.getAsignatura().getId(),
                p.getAsignatura().getNombre(), respuestas);
    }

    public void eliminarPregunta(Long id) throws Exception {
        if (!preguntaRepository.existsById(id))
            throw new IllegalArgumentException("Pregunta no encontrada");

        // Eliminar imágenes físicas asociadas
        Pregunta p = preguntaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada"));

        if (p.getImagenUrl() != null && !p.getImagenUrl().isBlank()) {
            for (String url : p.getImagenUrl().split(" \\| ")) {
                String rutaRelativa = url.startsWith("/") ? url.substring(1) : url;
                Path imagen = Path.of(imagenesPath, rutaRelativa).toAbsolutePath();
                Files.deleteIfExists(imagen);
                // Eliminar carpeta padre si está vacía
                Path carpeta = imagen.getParent();
                if (carpeta != null && Files.isDirectory(carpeta)) {
                    try (var contenido = Files.list(carpeta)) {
                        if (contenido.findAny().isEmpty()) {
                            Files.deleteIfExists(carpeta);
                        }
                    }
                }
            }
        }

        entityManager.createNativeQuery(
                "DELETE FROM respuestas_jugador WHERE respuesta_id IN (SELECT id FROM respuestas WHERE pregunta_id = :id)")
                .setParameter("id", id)
                .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM preguntas_partida WHERE pregunta_id = :id")
                .setParameter("id", id)
                .executeUpdate();

        entityManager.createNativeQuery("ALTER TABLE respuestas DISABLE TRIGGER trg_comprobar_respuesta_pregunta_del")
                .executeUpdate();

        respuestaRepository.deleteByPregunta_Id(id);

        entityManager.createNativeQuery("ALTER TABLE respuestas ENABLE TRIGGER trg_comprobar_respuesta_pregunta_del")
                .executeUpdate();

        preguntaRepository.deleteById(id);
    }

    public String subirImagen(MultipartFile file, String rutaDestino) throws Exception {
        String rutaRelativa = rutaDestino.startsWith("/") ? rutaDestino.substring(1) : rutaDestino;
        Path destino = Path.of(imagenesPath, rutaRelativa).toAbsolutePath();
        Files.createDirectories(destino.getParent());
        Files.copy(file.getInputStream(), destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return rutaDestino;
    }

    public void descomprimirImagenes(MultipartFile zipFile) throws Exception {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(zipFile.getInputStream())) {
            java.util.zip.ZipEntry entrada;
            // Recorre cada entrada del ZIP hasta que no queden más
            while ((entrada = zis.getNextEntry()) != null) {
                // Ignoramos las entradas que son carpetas, solo procesamos archivos
                if (entrada.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                // Ruta relativa del archivo dentro del ZIP (ej:
                // imagenes/cardiologia/pulmon.png)
                String nombreEntrada = entrada.getName();
                // Ruta absoluta donde se guardará el archivo
                Path destino = Path.of(imagenesPath, nombreEntrada).toAbsolutePath().normalize();
                // Seguridad anti zip-slip: descartamos rutas que salgan de imagenesPath
                if (!destino.startsWith(Path.of(imagenesPath).toAbsolutePath())) {
                    zis.closeEntry();
                    continue;
                }
                // Crea las carpetas intermedias si no existen
                Files.createDirectories(destino.getParent());
                // Copia el archivo del ZIP a la ruta destino, sobreescribiendo si ya existe
                Files.copy(zis, destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                zis.closeEntry();
            }
        }
    }

    public void eliminarImagen(String url) throws Exception {
        String rutaRelativa = url.startsWith("/") ? url.substring(1) : url;
        Path imagen = Path.of(imagenesPath, rutaRelativa).toAbsolutePath();
        Files.deleteIfExists(imagen);
        Path carpeta = imagen.getParent();
        if (carpeta != null && Files.isDirectory(carpeta)) {
            try (var contenido = Files.list(carpeta)) {
                if (contenido.findAny().isEmpty()) {
                    Files.deleteIfExists(carpeta);
                }
            }
        }
    }

}
