package com.jugamir.backend.service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;

final class RasterImages {
    private RasterImages() {}

    static void sanitize(Path file, String name) throws IOException {
        String lower = name.toLowerCase(Locale.ROOT);
        String format = lower.endsWith(".png") ? "png" :
                (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) ? "jpeg" : null;
        if (format == null) throw new IllegalArgumentException("Solo se admiten imágenes PNG y JPEG");
        BufferedImage image;
        try (var input = ImageIO.createImageInputStream(file.toFile())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("El archivo no es una imagen válida");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                if (!format.equalsIgnoreCase(reader.getFormatName()))
                    throw new IllegalArgumentException("La extensión no coincide con el contenido de la imagen");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 8192 || height > 8192 || (long) width * height > 16_000_000)
                    throw new IllegalArgumentException("La imagen supera las dimensiones permitidas");
                image = reader.read(0);
            } finally { reader.dispose(); }
        }
        Path clean = Files.createTempFile(file.getParent(), "raster-", ".tmp");
        try {
            // Re-encode pixels only: no uploaded scripts, ancillary chunks or metadata survive.
            if (!ImageIO.write(image, format, clean.toFile())) throw new IOException("No se pudo convertir la imagen");
            Files.move(clean, file, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(clean); }
    }
}
