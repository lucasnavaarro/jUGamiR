package com.jugamir.backend.service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.io.*;
import java.util.zip.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
class ZipLimitsTest {
    @TempDir Path dir;
    byte[] png() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB), "png", out); return out.toByteArray();
    }
    @Test void rejectedArchivePublishesNothing() throws Exception {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("imagenes/good.png")); zip.write(png()); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("imagenes/bomb.png"));
            byte[] block = new byte[8192];
            for (int i=0; i<1400; i++) zip.write(block);
            zip.closeEntry();
        }
        var storage = new ImageStorage(dir.toString());
        assertThrows(IllegalArgumentException.class, () -> storage.uploadZip(new MockMultipartFile("zip",out.toByteArray())));
        assertFalse(Files.exists(dir.resolve("imagenes/good.png")));
    }
    @Test void rejectsTooManyEntriesAndTraversal() throws Exception {
        var storage = new ImageStorage(dir.toString());
        for (boolean traversal : new boolean[]{true,false}) {
            var out = new ByteArrayOutputStream();
            try (var zip = new ZipOutputStream(out)) {
                int count = traversal ? 1 : 501;
                for (int i=0; i<count; i++) {
                    zip.putNextEntry(new ZipEntry(traversal ? "imagenes/../../escape.png" : "imagenes/dir"+i+"/")); zip.closeEntry();
                }
            }
            assertThrows(IllegalArgumentException.class, () -> storage.uploadZip(new MockMultipartFile("zip",out.toByteArray())));
        }
    }
    @Test void acceptsValidArchiveIncludingRootDirectory() throws Exception {
        var out = new ByteArrayOutputStream();
        try(var zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("imagenes/")); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("imagenes/ok.png")); zip.write(png()); zip.closeEntry();
        }
        new ImageStorage(dir.toString()).uploadZip(new MockMultipartFile("zip",out.toByteArray()));
        assertEquals(2, ImageIO.read(dir.resolve("imagenes/ok.png").toFile()).getWidth());
    }
}
