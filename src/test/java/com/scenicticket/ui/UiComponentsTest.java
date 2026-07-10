package com.scenicticket.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UiComponentsTest {
    @Test
    void primaryButtonPaintsItsOwnBackgroundInsteadOfNativeWhiteOverlay() {
        JButton button = UiComponents.primaryButton("查询");
        button.setSize(120, 36);
        BufferedImage image = new BufferedImage(120, 36, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        button.paint(graphics);
        graphics.dispose();

        Color paintedBackground = new Color(image.getRGB(10, 10), true);
        assertEquals("查询", button.getText());
        assertNotEquals(Color.WHITE.getRGB(), paintedBackground.getRGB());
        assertEquals(UiTheme.PRIMARY.getRGB(), paintedBackground.getRGB());
    }
}
