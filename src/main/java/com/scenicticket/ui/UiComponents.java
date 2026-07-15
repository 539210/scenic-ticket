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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public final class UiComponents {
    private static final String SELECTED_STYLE = "scenic.selectedStyle";
    private UiComponents() {
    }

    public static JButton primaryButton(String text) {
        return button(text, UiTheme.PRIMARY, Color.WHITE, UiTheme.PRIMARY.darker());
    }

    public static JButton secondaryButton(String text) {
        return button(text, Color.WHITE, UiTheme.TEXT, UiTheme.BORDER);
    }

    public static JButton dangerButton(String text) {
        return button(text, UiTheme.DANGER, Color.WHITE, UiTheme.DANGER.darker());
    }

    public static void setSelectedStyle(JButton button, boolean selected) {
        if (button == null) {
            return;
        }
        button.putClientProperty(SELECTED_STYLE, selected);
        button.repaint();
    }

    static boolean isSelectedStyle(JButton button) {
        return button != null && Boolean.TRUE.equals(button.getClientProperty(SELECTED_STYLE));
    }

    private static JButton button(String text, Color background, Color foreground, Color border) {
        JButton button = new StyledButton(text, background, foreground, border);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setPreferredSize(new Dimension(Math.max(88, button.getPreferredSize().width), 36));
        return button;
    }

    private static final class StyledButton extends JButton {
        private final Color normalBackground;
        private final Color normalForeground;
        private final Color borderColor;

        private StyledButton(String text, Color background, Color foreground, Color borderColor) {
            super(text);
            this.normalBackground = background;
            this.normalForeground = foreground;
            this.borderColor = borderColor;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Boolean selectedStyle = (Boolean) getClientProperty(SELECTED_STYLE);
                Color background = selectedStyle == null ? normalBackground
                        : selectedStyle ? UiTheme.PRIMARY : Color.WHITE;
                Color foreground = selectedStyle == null ? normalForeground
                        : selectedStyle ? Color.WHITE : UiTheme.TEXT;
                Color activeBorder = selectedStyle == null ? borderColor
                        : selectedStyle ? UiTheme.PRIMARY.darker() : UiTheme.BORDER;
                if (!isEnabled()) {
                    background = new Color(229, 231, 235);
                    foreground = UiTheme.MUTED;
                } else if (getModel().isPressed()) {
                    background = background.darker();
                } else if (getModel().isRollover()) {
                    background = blend(normalBackground, Color.BLACK, 0.08f);
                }
                g2.setColor(background);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(isEnabled() ? activeBorder : UiTheme.BORDER);
                g2.drawRect(0, 0, Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1));
                if (hasFocus() && isEnabled()) {
                    g2.setColor(blend(activeBorder, Color.BLACK, 0.18f));
                    g2.drawRect(2, 2, Math.max(0, getWidth() - 5), Math.max(0, getHeight() - 5));
                }
                g2.setFont(getFont());
                g2.setColor(foreground);
                FontMetrics metrics = g2.getFontMetrics();
                int x = (getWidth() - metrics.stringWidth(getText())) / 2;
                int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                g2.drawString(getText(), Math.max(0, x), y);
            } finally {
                g2.dispose();
            }
        }

        private static Color blend(Color base, Color overlay, float amount) {
            float safeAmount = Math.max(0f, Math.min(1f, amount));
            int red = Math.round(base.getRed() * (1f - safeAmount) + overlay.getRed() * safeAmount);
            int green = Math.round(base.getGreen() * (1f - safeAmount) + overlay.getGreen() * safeAmount);
            int blue = Math.round(base.getBlue() * (1f - safeAmount) + overlay.getBlue() * safeAmount);
            return new Color(red, green, blue);
        }
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

    public static JPanel busyGlassPane() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setFocusTraversalKeysEnabled(false);
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                event.consume();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                event.consume();
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                event.consume();
            }
        });
        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                event.consume();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                event.consume();
            }
        });
        panel.addMouseWheelListener((MouseWheelEvent event) -> event.consume());
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                event.consume();
            }

            @Override
            public void keyReleased(KeyEvent event) {
                event.consume();
            }

            @Override
            public void keyTyped(KeyEvent event) {
                event.consume();
            }
        });
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
