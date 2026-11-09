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
    @Test void appliesExifOrientationBeforeRemovingMetadata() throws Exception {
        var jpeg = new java.io.ByteArrayOutputStream();
        BufferedImage pixels = new BufferedImage(32,24,BufferedImage.TYPE_INT_RGB);
        ImageIO.write(pixels,"jpeg",jpeg);
        byte[] original=jpeg.toByteArray();
        var exif=new java.io.ByteArrayOutputStream();
        exif.write(new byte[]{'E','x','i','f',0,0});
        var tiff=java.nio.ByteBuffer.allocate(26).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        tiff.put((byte)'I').put((byte)'I').putShort((short)42).putInt(8).putShort((short)1);
        tiff.putShort((short)0x112).putShort((short)3).putInt(1).putShort((short)6).putShort((short)0).putInt(0);
        exif.write(tiff.array());
        var out=new java.io.ByteArrayOutputStream();out.write(original,0,2);
        var data=new java.io.DataOutputStream(out);data.writeShort(0xffe1);data.writeShort(exif.size()+2);data.write(exif.toByteArray());
        out.write(original,2,original.length-2);
        Path file=dir.resolve("oriented.jpg");Files.write(file,out.toByteArray());
        RasterImages.sanitize(file,"oriented.jpg");
        BufferedImage result=ImageIO.read(file.toFile());assertEquals(24,result.getWidth());assertEquals(32,result.getHeight());
        assertFalse(new String(Files.readAllBytes(file),java.nio.charset.StandardCharsets.ISO_8859_1).contains("Exif"));
    }
}
