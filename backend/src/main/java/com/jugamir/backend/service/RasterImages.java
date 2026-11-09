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
                if (format.equals("jpeg")) image = orient(image, orientation(file));
            } finally { reader.dispose(); }
        }
        Path clean = Files.createTempFile(file.getParent(), "raster-", ".tmp");
        try {
            // Re-encode pixels only: no uploaded scripts, ancillary chunks or metadata survive.
            if (!ImageIO.write(image, format, clean.toFile())) throw new IOException("No se pudo convertir la imagen");
            Files.move(clean, file, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(clean); }
    }

    private static int orientation(Path file) throws IOException {
        try (var in = new java.io.DataInputStream(Files.newInputStream(file))) {
            if (in.readUnsignedShort() != 0xffd8) return 1;
            while (true) {
                int marker;
                do { marker = in.readUnsignedByte(); } while (marker != 0xff);
                do { marker = in.readUnsignedByte(); } while (marker == 0xff);
                if (marker == 0xda || marker == 0xd9) return 1;
                if (marker == 0x01 || marker >= 0xd0 && marker <= 0xd7) continue;
                int length = in.readUnsignedShort() - 2;
                if (length < 0) return 1;
                byte[] data = in.readNBytes(length);
                if (data.length != length) return 1;
                if (marker != 0xe1 || length < 14 || data[0] != 'E' || data[1] != 'x'
                        || data[2] != 'i' || data[3] != 'f' || data[4] != 0 || data[5] != 0) continue;
                var tiff = java.nio.ByteBuffer.wrap(data, 6, length - 6).slice();
                if (tiff.get(0) == 'I' && tiff.get(1) == 'I') tiff.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                else if (tiff.get(0) != 'M' || tiff.get(1) != 'M') return 1;
                if (tiff.getShort(2) != 42) return 1;
                int offset = tiff.getInt(4);
                if (offset < 8 || offset > tiff.limit() - 2) return 1;
                int count = Short.toUnsignedInt(tiff.getShort(offset));
                for (int i = 0; i < count; i++) {
                    long position = (long) offset + 2 + 12L * i;
                    if (position > tiff.limit() - 12) return 1;
                    int p = (int) position;
                    if (Short.toUnsignedInt(tiff.getShort(p)) == 0x112 && tiff.getShort(p + 2) == 3 && tiff.getInt(p + 4) == 1) {
                        int value = Short.toUnsignedInt(tiff.getShort(p + 8));
                        return value >= 1 && value <= 8 ? value : 1;
                    }
                }
            }
        } catch (java.io.EOFException ex) { return 1; }
    }

    private static BufferedImage orient(BufferedImage image, int orientation) {
        if (orientation == 1) return image;
        int w = image.getWidth(), h = image.getHeight();
        BufferedImage result = new BufferedImage(orientation >= 5 ? h : w, orientation >= 5 ? w : h,
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int dx = x, dy = y;
            switch (orientation) {
                case 2 -> dx = w - 1 - x;
                case 3 -> { dx = w - 1 - x; dy = h - 1 - y; }
                case 4 -> dy = h - 1 - y;
                case 5 -> { dx = y; dy = x; }
                case 6 -> { dx = h - 1 - y; dy = x; }
                case 7 -> { dx = h - 1 - y; dy = w - 1 - x; }
                case 8 -> { dx = y; dy = w - 1 - x; }
            }
            result.setRGB(dx, dy, image.getRGB(x, y));
        }
        return result;
    }
}
