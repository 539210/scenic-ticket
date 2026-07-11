package com.scenicticket.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void busyGlassPaneConsumesMouseWheelAndKeyEvents() {
        JPanel glassPane = UiComponents.busyGlassPane();

        MouseEvent mouse = new MouseEvent(glassPane, MouseEvent.MOUSE_PRESSED, 1L, 0, 10, 10, 1, false);
        glassPane.getMouseListeners()[0].mousePressed(mouse);
        assertTrue(mouse.isConsumed());

        MouseWheelEvent wheel = new MouseWheelEvent(glassPane, MouseEvent.MOUSE_WHEEL, 1L, 0,
                10, 10, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 1);
        glassPane.getMouseWheelListeners()[0].mouseWheelMoved(wheel);
        assertTrue(wheel.isConsumed());

        KeyEvent key = new KeyEvent(glassPane, KeyEvent.KEY_PRESSED, 1L, 0, KeyEvent.VK_ENTER, '\n');
        glassPane.getKeyListeners()[0].keyPressed(key);
        assertTrue(key.isConsumed());
    }
}
