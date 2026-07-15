package com.scenicticket.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenicImageStorageTest {
    @Test
    void copiesDroppedImageIntoManagedDirectory(@TempDir Path tempDir) throws Exception {
        Path source = tempDir.resolve("source.png");
        javax.imageio.ImageIO.write(new BufferedImage(30, 20, BufferedImage.TYPE_INT_RGB),
                "png", source.toFile());
        Path managed = tempDir.resolve("managed");

        List<String> stored = new ScenicImageStorage(managed).store(List.of(source.toFile()));

        assertEquals(1, stored.size());
        assertTrue(Files.isRegularFile(Path.of(stored.get(0))));
        assertTrue(Path.of(stored.get(0)).startsWith(managed));
    }

    @Test
    void rejectsNonImageFiles(@TempDir Path tempDir) throws Exception {
        Path text = tempDir.resolve("fake.png");
        Files.writeString(text, "not an image");

        assertThrows(IllegalArgumentException.class,
                () -> new ScenicImageStorage(tempDir.resolve("managed")).store(List.of(text.toFile())));
    }
}
