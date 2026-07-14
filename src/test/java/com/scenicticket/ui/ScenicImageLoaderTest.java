package com.scenicticket.ui;

import org.junit.jupiter.api.Test;

import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenicImageLoaderTest {
    @Test
    void scalesLargeImagesProportionallyAndDoesNotUpscaleSmallImages() {
        ImageIcon large = ScenicImageLoader.scale(
                new BufferedImage(1200, 600, BufferedImage.TYPE_INT_RGB), 360, 220);
        ImageIcon small = ScenicImageLoader.scale(
                new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB), 360, 220);

        assertEquals(360, large.getIconWidth());
        assertEquals(180, large.getIconHeight());
        assertEquals(120, small.getIconWidth());
        assertEquals(80, small.getIconHeight());
    }

    @Test
    void emptyOrUnsupportedImageSourcesAreTreatedAsNoImage() {
        ScenicImageLoader loader = new ScenicImageLoader(360, 220);

        assertTrue(loader.loadFirst(List.of()).isEmpty());
        assertTrue(loader.loadFirst(List.of("file:///tmp/scenic.jpg", "not-a-url")).isEmpty());
    }
}
