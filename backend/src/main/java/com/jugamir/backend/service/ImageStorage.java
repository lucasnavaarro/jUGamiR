package com.jugamir.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.zip.ZipInputStream;

@Service
public class ImageStorage {
    private final Path root;
    static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    static final long MAX_ZIP_BYTES = 200 * 1024 * 1024;
    static final int MAX_ENTRIES = 500;

    private long copyBounded(InputStream input, Path stage, long limit, long deadline) throws IOException {
        long size = 0;
        try (var output = Files.newOutputStream(stage)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) != -1) {
                size += n;
                if (size > limit || System.nanoTime() > deadline)
                    throw new IllegalArgumentException("El archivo supera los límites de tamaño o tiempo");
                output.write(buffer, 0, n);
            }
        }
        return size;
    }

    public ImageStorage(@Value("${imagenes.path:src/main/resources/static}") String base) {
        this.root = Path.of(base).toAbsolutePath().normalize().resolve("imagenes");
    }

    Path destination(String name) throws IOException {
        if (name == null || name.isBlank() || name.contains("\\") || name.indexOf('\0') >= 0)
            throw new IllegalArgumentException("Ruta de imagen inválida");
        String relative = name.startsWith("/") ? name.substring(1) : name;
        if (!relative.startsWith("imagenes/"))
            throw new IllegalArgumentException("La ruta debe estar dentro de imagenes/");
        Path suffix = Path.of(relative.substring("imagenes/".length()));
        if (suffix.isAbsolute()) throw new IllegalArgumentException("Ruta absoluta no permitida");
        for (Path part : suffix) {
            if (part.toString().equals("..") || part.toString().equals("."))
                throw new IllegalArgumentException("Ruta de imagen inválida");
        }
        Path target = root.resolve(suffix).normalize();
        if (!target.startsWith(root) || target.equals(root))
            throw new IllegalArgumentException("Ruta fuera del almacén de imágenes");
        // Reject links at every existing component, including the storage root.
        Path current = target.getRoot();
        for (Path part : target) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current))
                throw new IllegalArgumentException("No se permiten enlaces simbólicos");
        }
        return target;
    }

    void publish(Path staged, String name) throws IOException {
        Path target = destination(name);
        Files.createDirectories(target.getParent());
        target = destination(name);
        Path temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            Files.copy(staged, temporary, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public String upload(MultipartFile file, String name) throws IOException {
        destination(name);
        Path stage = Files.createTempFile("jugamir-image-", ".tmp");
        try (InputStream input = file.getInputStream()) {
            copyBounded(input, stage, MAX_IMAGE_BYTES, System.nanoTime() + 30_000_000_000L);
            RasterImages.sanitize(stage, name);
            if (Files.size(stage) > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Imagen demasiado grande");
            publish(stage, name);
            return name.startsWith("/") ? name.substring(1) : name;
        } finally {
            Files.deleteIfExists(stage);
        }
    }

    public void uploadZip(MultipartFile file) throws IOException {
        var stages = new java.util.LinkedHashMap<String, Path>();
        long total = 0;
        long sanitizedTotal = 0;
        long deadline = System.nanoTime() + 30_000_000_000L;
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > MAX_ENTRIES || System.nanoTime() > deadline)
                    throw new IllegalArgumentException("El ZIP supera los límites de entradas o tiempo");
                destination(entry.isDirectory() ? entry.getName() + "directory-check.png" : entry.getName());
                if (entry.isDirectory()) {
                    // Even a directory entry may carry compressed data.
                    if (zip.read() != -1) throw new IllegalArgumentException("Directorio ZIP con contenido");
                    continue;
                }
                if (stages.containsKey(entry.getName())) throw new IllegalArgumentException("Entrada ZIP duplicada");
                Path stage = Files.createTempFile("jugamir-zip-", ".tmp");
                stages.put(entry.getName(), stage);
                long size = copyBounded(zip, stage, Math.min(MAX_IMAGE_BYTES, MAX_ZIP_BYTES - total), deadline);
                total += size;
                if (entry.getCompressedSize() > 0 && size > entry.getCompressedSize() * 200L)
                    throw new IllegalArgumentException("Compresión ZIP excesiva");
                RasterImages.sanitize(stage, entry.getName());
                long sanitizedSize = Files.size(stage);
                sanitizedTotal += sanitizedSize;
                if (sanitizedSize > MAX_IMAGE_BYTES || sanitizedTotal > MAX_ZIP_BYTES || System.nanoTime() > deadline)
                    throw new IllegalArgumentException("Las imágenes del ZIP superan los límites");
            }
            if (stages.isEmpty()) throw new IllegalArgumentException("El ZIP no contiene imágenes");
            // Validate the complete archive before publishing any image.
            for (var staged : stages.entrySet()) publish(staged.getValue(), staged.getKey());
        } finally {
            for (Path stage : stages.values()) Files.deleteIfExists(stage);
        }
    }

    public void delete(String name) throws IOException {
        Path target = destination(name);
        if (Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("Se esperaba un archivo de imagen");
        Files.deleteIfExists(target);
    }
}
