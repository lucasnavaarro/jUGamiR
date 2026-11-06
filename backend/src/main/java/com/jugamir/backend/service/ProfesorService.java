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
    private final ImageStorage imageStorage;

    @Value("${scripts.path:db}")
    private String scriptsPath;

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

        Asignatura asignatura = asignaturaRepository.findLockedById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe esa asignatura"));

        if (!asignatura.getCategoria().getId().equals(request.categoriaId()) &&
                entityManager.createQuery("select count(p) from PreguntaPartida p where p.pregunta.asignatura.id = :id", Long.class)
                        .setParameter("id", id).getSingleResult() > 0)
            throw new IllegalStateException("La asignatura tiene preguntas utilizadas; su categoría debe conservarse para mantener el historial");

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

        if (preguntaRepository.existsByAsignatura_Id(id))
            throw new IllegalStateException("La asignatura contiene preguntas; reasígnalas antes de eliminarla");
        asignaturaRepository.deleteById(id);
    }

    public ImportarResultadoDTO importarPreguntas(MultipartFile file, Long usuarioId) throws Exception {

        Path tempFile = Files.createTempFile("preguntas_import_", ".csv");
        try {
            file.transferTo(tempFile.toFile());
            String outputPreguntas = ejecutarScript("import_csv.py", tempFile.toString(), usuarioId);

            int insertadas = extraerNumero(outputPreguntas, "Insertadas");
            int duplicadas = extraerNumero(outputPreguntas, "Duplicadas");
            int errores = extraerNumero(outputPreguntas, "Omitidas");

            List<String> detalle = new ArrayList<>();
            detalle.add(outputPreguntas.trim());

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
        var reader = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try { return new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8); }
            catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
        });
        try {
            if (!process.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new IllegalStateException("La importación superó el tiempo máximo; comprueba el resultado antes de reintentar");
            }
            String output = reader.get(5, java.util.concurrent.TimeUnit.SECONDS);
            if (process.exitValue() != 0)
                throw new IllegalArgumentException("No se pudo importar el CSV. Revisa el formato y las columnas obligatorias");
            return output;
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            reader.cancel(true);
        }
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
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException("La página debe ser positiva y el tamaño debe estar entre 1 y 100");
        Pageable pageable = PageRequest.of(page, size);
        Page<Pregunta> resultado = preguntaRepository.buscarPorIdentificadorOEnunciado(q, pageable);
        List<PreguntaResumenDTO> dtos = resultado.getContent().stream()
                .map(this::mapToDTO)
                .toList();
        return Map.of("preguntas", dtos, "totalPaginas", resultado.getTotalPages(), "totalElementos",
                resultado.getTotalElements());
    }

    public PreguntaResumenDTO editarPregunta(Long id, EditarPreguntaRequest request) {

        Pregunta p = preguntaRepository.findLockedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada"));

        if (preguntaUtilizada(id))
            throw new IllegalStateException("La pregunta ya se utilizó en una partida y debe conservar su contenido y solución");
        if (request.enunciado() == null || request.enunciado().isBlank() || request.dificultad() == null || request.asignaturaId() == null)
            throw new IllegalArgumentException("Enunciado, dificultad y asignatura son obligatorios");
        if (request.identificador() != null && request.identificador().length() > 60)
            throw new IllegalArgumentException("El identificador supera 60 caracteres");
        if (request.imagenUrl() != null && request.imagenUrl().length() > 2048)
            throw new IllegalArgumentException("La ruta de imagen supera 2048 caracteres");
        // Verificar identificador duplicado (ignorando la pregunta actual)
        if (!java.util.Objects.equals(p.getIdentificador(), request.identificador()) &&
                preguntaRepository.existsByIdentificador(request.identificador()))
            throw new IllegalArgumentException("Ya existe una pregunta con ese identificador");

        // Verificar enunciado duplicado (ignorando la pregunta actual)
        if (!p.getEnunciado().equals(request.enunciado()) &&
                preguntaRepository.existsByEnunciado(request.enunciado()))
            throw new IllegalArgumentException("Ya existe una pregunta con ese enunciado");

        Asignatura asignatura = asignaturaRepository.findById(request.asignaturaId())
                .orElseThrow(() -> new IllegalArgumentException("Asignatura no encontrada"));

        Map<Long, Respuesta> opciones = new java.util.HashMap<>();
        if (request.respuestas() != null) {
            for (RespuestaDTO dto : request.respuestas()) {
                if (dto == null || dto.id() == null || opciones.containsKey(dto.id()))
                    throw new IllegalArgumentException("Las opciones deben tener identificadores válidos y únicos");
                Respuesta respuesta = respuestaRepository.findByIdAndPregunta_Id(dto.id(), id)
                        .orElseThrow(() -> new IllegalArgumentException("La respuesta no pertenece a la pregunta"));
                if (dto.texto() == null || dto.texto().isBlank())
                    throw new IllegalArgumentException("El texto de las opciones es obligatorio");
                opciones.put(dto.id(), respuesta);
            }
        }

        if (request.respuestas() != null) {
            var existentes = respuestaRepository.findByPregunta(p);
            long correctas = existentes.stream().filter(r -> opciones.containsKey(r.getId())
                    ? request.respuestas().stream().anyMatch(d -> d.id().equals(r.getId()) && d.esCorrecta())
                    : r.getEsCorrecta()).count();
            if (correctas != 1) throw new IllegalArgumentException("Debe haber exactamente una opción correcta");
        }
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
                    Respuesta r = opciones.get(dto.id());
                    r.setTextoRespuesta(dto.texto());
                    r.setEsCorrecta(true);
                    respuestaRepository.save(r);
                }
            }
            // Luego actualizamos las incorrectas
            for (RespuestaDTO dto : request.respuestas()) {
                if (!dto.esCorrecta()) {
                    Respuesta r = opciones.get(dto.id());
                    r.setTextoRespuesta(dto.texto());
                    r.setEsCorrecta(false);
                    respuestaRepository.save(r);
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

        Pregunta p = preguntaRepository.findLockedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada"));
        if (preguntaUtilizada(id)) {
            // Retire from future draws without erasing historical answers or their images.
            p.setAnulada(true);
            preguntaRepository.save(p);
            return;
        }
        preguntaRepository.delete(p);
    }

    private boolean preguntaUtilizada(Long id) {
        return entityManager.createQuery("select count(p) from PreguntaPartida p where p.pregunta.id = :id", Long.class)
                .setParameter("id", id).getSingleResult() > 0;
    }

    public String subirImagen(MultipartFile file, String rutaDestino) throws Exception {
        lockImageReferences();
        requireUnreferencedImage(rutaDestino);
        return imageStorage.upload(file, rutaDestino);
    }

    public void descomprimirImagenes(MultipartFile zipFile) throws Exception {
        lockImageReferences();
        imageStorage.uploadZip(zipFile, this::requireUnreferencedImage);
    }

    public void eliminarImagen(String url) throws Exception {
        lockImageReferences();
        requireUnreferencedImage(url);
        imageStorage.delete(url);
    }

    private void lockImageReferences() {
        // Serialize reference changes/imports against physical image replacement/deletion.
        entityManager.createNativeQuery("LOCK TABLE preguntas IN SHARE MODE").executeUpdate();
    }

    private void requireUnreferencedImage(String name) {
        String target = imageStorage.canonicalName(name);
        for (String urls : entityManager.createQuery("select p.imagenUrl from Pregunta p where p.imagenUrl is not null", String.class).getResultList()) {
            for (String url : urls.split("\\s*\\|\\s*")) {
                String path;
                try { path = java.net.URI.create(url).normalize().getPath(); }
                catch (IllegalArgumentException ex) { path = url; }
                if (path == null || path.isBlank()) continue;
                String reference;
                try { reference = imageStorage.canonicalName(java.nio.file.Path.of(path).normalize().toString()); }
                catch (IllegalArgumentException ex) { continue; } // External or unsupported legacy reference.
                if (reference.equals(target))
                    throw new IllegalStateException("La imagen está utilizada por una pregunta; sube otra con una ruta nueva");
            }
        }
    }
}
