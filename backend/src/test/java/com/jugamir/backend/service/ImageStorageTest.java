package com.jugamir.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ImageStorageTest {
    @TempDir Path temp;

    @Test void rejectsTraversalForUploadAndDelete() throws Exception {
        ImageStorage storage = new ImageStorage(temp.toString());
        Path sentinel = temp.resolve("sentinel");
        Files.writeString(sentinel, "unchanged");
        for (String path : new String[]{"../sentinel", "imagenes/../../sentinel", "/etc/passwd", "imagenes/../sentinel"}) {
            assertThrows(IllegalArgumentException.class, () -> storage.upload(new MockMultipartFile("file", new byte[]{1}), path));
            assertThrows(IllegalArgumentException.class, () -> storage.delete(path));
        }
        assertEquals("unchanged", Files.readString(sentinel));
    }

    @Test void rejectsSymlinkParentsAndTargets() throws Exception {
        ImageStorage storage = new ImageStorage(temp.toString());
        Files.createDirectories(temp.resolve("imagenes"));
        Path outside = Files.createDirectory(temp.resolve("outside"));
        Files.createSymbolicLink(temp.resolve("imagenes/link"), outside);
        assertThrows(IllegalArgumentException.class, () -> storage.destination("imagenes/link/file.png"));
        assertFalse(Files.exists(outside.resolve("file.png")));
    }
    @Test void rejectsZipSymlinkAttributesBeforePublishingAnyFile() throws Exception {
        var pixels=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",pixels);
        var archive=new java.io.ByteArrayOutputStream();
        try(var zip=new java.util.zip.ZipOutputStream(archive)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("imagenes/link.png"));zip.write(pixels.toByteArray());zip.closeEntry();
        }
        byte[] data=archive.toByteArray();var bytes=java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for(int j=0;j<data.length-46;j++)if(bytes.getInt(j)==0x02014b50){bytes.putShort(j+4,(short)0x0314);bytes.putInt(j+38,0120777<<16);break;}
        var file=new MockMultipartFile("file","links.zip","application/zip",data);
        assertThrows(IllegalArgumentException.class,()->new ImageStorage(temp.toString()).uploadZip(file));
        assertFalse(Files.exists(temp.resolve("imagenes/link.png")));
    }
}
