package com.scenicticket.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;

public final class UiComponents {
    private UiComponents() {
    }

    public static JButton primaryButton(String text) {
        return button(text, UiTheme.PRIMARY, Color.WHITE);
    }

    public static JButton secondaryButton(String text) {
        JButton button = button(text, Color.WHITE, UiTheme.TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));
        return button;
    }

    public static JButton dangerButton(String text) {
        return button(text, UiTheme.DANGER, Color.WHITE);
    }

    private static JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setPreferredSize(new Dimension(Math.max(88, button.getPreferredSize().width), 36));
        return button;
    }

    public static JPanel page() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(UiTheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return panel;
    }

    public static JPanel card(String title, java.awt.Component content) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UiTheme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        if (title != null && !title.isBlank()) {
            JLabel heading = new JLabel(title);
            heading.setFont(UiTheme.SECTION_FONT);
            heading.setForeground(UiTheme.TEXT);
            panel.add(heading, BorderLayout.NORTH);
        }
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    public static JPanel toolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        panel.setOpaque(false);
        return panel;
    }

    public static JTable table(javax.swing.table.TableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        return table;
    }

    public static JTextArea readOnlyTextArea(int rows, int columns) {
        JTextArea area = new JTextArea(rows, columns);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(Color.WHITE);
        area.setForeground(UiTheme.TEXT);
        area.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return area;
    }

    public static JScrollPane scroll(java.awt.Component content) {
        JScrollPane pane = new JScrollPane(content);
        pane.getVerticalScrollBar().setUnitIncrement(18);
        pane.getHorizontalScrollBar().setUnitIncrement(18);
        return pane;
    }
}
