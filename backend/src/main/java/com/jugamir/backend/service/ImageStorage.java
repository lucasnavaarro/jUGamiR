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

    Path destination(String name) {
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

    String canonicalName(String name) {
        return "imagenes/" + root.relativize(destination(name)).toString().replace('\\', '/');
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
        uploadZip(file, name -> {});
    }

    public void uploadZip(MultipartFile file, java.util.function.Consumer<String> beforePublish) throws IOException {
        var stages = new java.util.LinkedHashMap<String, Path>();
        long total = 0;
        long sanitizedTotal = 0;
        long deadline = System.nanoTime() + 30_000_000_000L;
        int count = 0;
        Path archive = Files.createTempFile("jugamir-archive-", ".zip");
        try (InputStream input = file.getInputStream()) {
            copyBounded(input, archive, MAX_ZIP_BYTES + 1024 * 1024, deadline);
            rejectSpecialEntries(archive);
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
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
                String canonical = canonicalName(entry.getName());
                if (stages.containsKey(canonical)) throw new IllegalArgumentException("Entrada ZIP duplicada");
                Path stage = Files.createTempFile("jugamir-zip-", ".tmp");
                stages.put(canonical, stage);
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
            for (String name : stages.keySet()) beforePublish.accept(name);
            for (var staged : stages.entrySet()) publish(staged.getValue(), staged.getKey());
            }
        } finally {
            Files.deleteIfExists(archive);
            for (Path stage : stages.values()) Files.deleteIfExists(stage);
        }
    }

    // ZipInputStream does not expose Unix attributes; inspect the central directory first.
    private void rejectSpecialEntries(Path archive) throws IOException {
        try (var file = new java.io.RandomAccessFile(archive.toFile(), "r")) {
            int tailSize = (int) Math.min(file.length(), 65557);
            byte[] tail = new byte[tailSize];
            file.seek(file.length() - tailSize);
            file.readFully(tail);
            var end = java.nio.ByteBuffer.wrap(tail).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            int eocd = -1;
            for (int i = tailSize - 22; i >= 0; i--) {
                if (end.getInt(i) == 0x06054b50 && i + 22 + Short.toUnsignedInt(end.getShort(i + 20)) == tailSize) { eocd = i; break; }
            }
            if (eocd < 0) throw new IllegalArgumentException("ZIP inválido");
            int entries = Short.toUnsignedInt(end.getShort(eocd + 10));
            if (end.getShort(eocd + 4) != 0 || end.getShort(eocd + 6) != 0 || entries == 65535 || entries > MAX_ENTRIES)
                throw new IllegalArgumentException("ZIP dividido, ZIP64 o demasiadas entradas no admitido");
            long position = Integer.toUnsignedLong(end.getInt(eocd + 16));
            for (int i = 0; i < entries; i++) {
                file.seek(position);
                byte[] header = new byte[46]; file.readFully(header);
                var h = java.nio.ByteBuffer.wrap(header).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                if (h.getInt(0) != 0x02014b50) throw new IllegalArgumentException("Directorio ZIP inválido");
                int mode = (h.getInt(38) >>> 16) & 0170000;
                if (mode != 0 && mode != 0100000 && mode != 0040000)
                    throw new IllegalArgumentException("No se admiten enlaces ni archivos especiales en ZIP");
                position += 46L + Short.toUnsignedInt(h.getShort(28)) + Short.toUnsignedInt(h.getShort(30)) + Short.toUnsignedInt(h.getShort(32));
            }
        } catch (java.io.EOFException ex) { throw new IllegalArgumentException("ZIP truncado", ex); }
    }

    public void delete(String name) throws IOException {
        Path target = destination(name);
        if (Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("Se esperaba un archivo de imagen");
        Files.deleteIfExists(target);
    }
}
