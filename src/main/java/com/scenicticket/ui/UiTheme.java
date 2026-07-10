package com.scenicticket.ui;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;

public final class UiTheme {
    public static final Color BACKGROUND = new Color(245, 247, 250);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(218, 223, 230);
    public static final Color TEXT = new Color(31, 41, 55);
    public static final Color MUTED = new Color(102, 112, 133);
    public static final Color PRIMARY = new Color(15, 118, 110);
    public static final Color DANGER = new Color(185, 28, 28);
    public static final Font BODY_FONT = new Font("Microsoft YaHei UI", Font.PLAIN, 14);
    public static final Font TITLE_FONT = new Font("Microsoft YaHei UI", Font.BOLD, 24);
    public static final Font SECTION_FONT = new Font("Microsoft YaHei UI", Font.BOLD, 16);

    private UiTheme() {
    }

    public static void install() {
        FontUIResource font = new FontUIResource(BODY_FONT);
        UIManager.put("Label.font", font);
        UIManager.put("Button.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("PasswordField.font", font);
        UIManager.put("TextArea.font", font);
        UIManager.put("ComboBox.font", font);
        UIManager.put("Table.font", font);
        UIManager.put("TableHeader.font", new FontUIResource(BODY_FONT.deriveFont(Font.BOLD)));
        UIManager.put("TabbedPane.font", font);
        UIManager.put("OptionPane.messageFont", font);
        UIManager.put("OptionPane.buttonFont", font);
        UIManager.put("Panel.background", new ColorUIResource(BACKGROUND));
        UIManager.put("Table.selectionBackground", new ColorUIResource(new Color(204, 251, 241)));
        UIManager.put("Table.selectionForeground", new ColorUIResource(TEXT));
        UIManager.put("Table.gridColor", new ColorUIResource(new Color(232, 235, 240)));
        UIManager.put("TableHeader.background", new ColorUIResource(new Color(241, 245, 249)));
        UIManager.put("TableHeader.foreground", new ColorUIResource(TEXT));
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BORDER));
    }
}
