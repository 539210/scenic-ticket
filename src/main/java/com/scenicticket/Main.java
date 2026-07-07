package com.scenicticket;

import com.scenicticket.ui.AppFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                AppFrame frame = new AppFrame();
                frame.setVisible(true);
                log.info("Scenic Ticket Swing application started.");
            } catch (Exception e) {
                log.error("Failed to start Scenic Ticket application.", e);
                JOptionPane.showMessageDialog(null, e.getMessage(), "启动失败", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
