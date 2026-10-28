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
}
