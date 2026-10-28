package com.jugamir.backend.service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
class RasterImagesTest {
    @TempDir Path dir;
    @Test void rejectsActiveFilesAndFakeExtensions() throws Exception {
        Path file = dir.resolve("payload");
        Files.writeString(file, "<html><script>alert(1)</script></html>");
        assertThrows(IllegalArgumentException.class, () -> RasterImages.sanitize(file, "image.html"));
        assertThrows(IllegalArgumentException.class, () -> RasterImages.sanitize(file, "image.png"));
        assertThrows(IllegalArgumentException.class, () -> RasterImages.sanitize(file, "image.svg"));
    }
    @Test void stripsAppendedActiveContentAndKeepsImage() throws Exception {
        Path file = dir.resolve("valid");
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", file.toFile());
        Files.writeString(file, "<script>attack()</script>", StandardOpenOption.APPEND);
        RasterImages.sanitize(file, "image.png");
        assertEquals(2, ImageIO.read(file.toFile()).getWidth());
        assertFalse(new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.ISO_8859_1).contains("<script>"));
    }
}
