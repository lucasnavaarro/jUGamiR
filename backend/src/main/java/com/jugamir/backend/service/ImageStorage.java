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
            Files.copy(input, stage, StandardCopyOption.REPLACE_EXISTING);
            RasterImages.sanitize(stage, name);
            publish(stage, name);
            return name.startsWith("/") ? name.substring(1) : name;
        } finally { Files.deleteIfExists(stage); }
    }

    public void uploadZip(MultipartFile file) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                destination(entry.getName());
                Path stage = Files.createTempFile("jugamir-zip-", ".tmp");
                try {
                    Files.copy(zip, stage, StandardCopyOption.REPLACE_EXISTING);
                    RasterImages.sanitize(stage, entry.getName());
                    publish(stage, entry.getName());
                } finally { Files.deleteIfExists(stage); }
            }
        }
    }

    public void delete(String name) throws IOException {
        Path target = destination(name);
        if (Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("Se esperaba un archivo de imagen");
        Files.deleteIfExists(target);
    }
}
