package com.scenicticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            log.info("Scenic Ticket application started.");
            System.out.println("Scenic Ticket system initialized. UI modules will be added in later days.");
        });
    }
}
