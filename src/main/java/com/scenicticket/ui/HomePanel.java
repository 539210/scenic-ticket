package com.scenicticket.ui;

import com.scenicticket.model.User;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.Consumer;

public final class HomePanel extends JPanel {
    private static final String NOT_LOGGED_IN = "\u672a\u767b\u5f55";

    private final User user;
    private final Consumer<String> navigation;
    private final Consumer<String> statusSink;
    private final JLabel accountValue = new JLabel(NOT_LOGGED_IN);
    private final JLabel roleValue = new JLabel("-");
    private final JTextArea summaryArea = UiComponents.readOnlyTextArea(10, 80);

    public HomePanel(User user, Consumer<String> navigation, Consumer<String> statusSink) {
        super(new BorderLayout(12, 12));
        this.user = user;
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.statusSink = Objects.requireNonNull(statusSink, "statusSink");
        setBackground(UiTheme.BACKGROUND);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        buildContent();
        refreshSummary();
    }

    private void buildContent() {
        JPanel metrics = new JPanel(new GridLayout(1, 2, 12, 12));
        metrics.setOpaque(false);
        metrics.add(metricCard("\u5f53\u524d\u8d26\u53f7", accountValue));
        metrics.add(metricCard("\u8d26\u53f7\u7c7b\u578b", roleValue));

        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quickActions.setOpaque(false);
        addNavigationButton(quickActions, "\u5b8c\u5584\u6863\u6848", "\u4e2a\u4eba\u6863\u6848");
        addNavigationButton(quickActions, "\u6d4f\u89c8\u666f\u70b9", "\u666f\u70b9\u6d4f\u89c8");
        addNavigationButton(quickActions, "\u6211\u7684\u8ba2\u5355", "\u6211\u7684\u8ba2\u5355");
        addNavigationButton(quickActions, "\u67e5\u770b\u62a5\u8868", "\u7edf\u8ba1\u62a5\u8868");

        JButton refreshButton = UiComponents.secondaryButton("\u5237\u65b0");
        refreshButton.addActionListener(event -> {
            refreshSummary();
            statusSink.accept("\u9996\u9875\u5df2\u5237\u65b0");
        });
        quickActions.add(refreshButton);

        if (isAdmin()) {
            addNavigationButton(quickActions, "\u540e\u53f0\u7ba1\u7406", "\u540e\u53f0\u7ba1\u7406");
            addNavigationButton(quickActions, "\u7cfb\u7edf\u5ba1\u8ba1", "\u7cfb\u7edf\u5ba1\u8ba1");
        }

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(metrics, BorderLayout.CENTER);
        top.add(quickActions, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(UiComponents.card("\u6b22\u8fce\u4f7f\u7528", summaryArea), BorderLayout.CENTER);
    }

    private void addNavigationButton(JPanel target, String text, String page) {
        JButton button = UiComponents.secondaryButton(text);
        button.addActionListener(event -> navigation.accept(page));
        target.add(button);
    }

    private JPanel metricCard(String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(UiTheme.BORDER),
                javax.swing.BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(UiTheme.MUTED);
        valueLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 18));
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }

    private void refreshSummary() {
        if (user == null) {
            accountValue.setText(NOT_LOGGED_IN);
            roleValue.setText("-");
            summaryArea.setText("\u8bf7\u767b\u5f55\u540e\u4f7f\u7528\u7cfb\u7edf\u3002");
            return;
        }
        accountValue.setText(user.getUsername() + " / ID " + user.getUserId());
        roleValue.setText(UiFormatters.role(user.getRole()));
        String summary = "\u4f60\u597d\uff0c" + user.getUsername()
                + "\u3002\u8bf7\u9009\u62e9\u4e0a\u65b9\u5feb\u6377\u5165\u53e3\u6216\u5de6\u4fa7\u5bfc\u822a\u5f00\u59cb\u4f7f\u7528\u3002";
        if (isAdmin()) {
            summary += System.lineSeparator()
                    + "\u5f53\u524d\u4e3a\u7ba1\u7406\u5458\u8d26\u6237\uff0c\u53ef\u4f7f\u7528\u540e\u53f0\u7ba1\u7406\u4e0e\u7cfb\u7edf\u5ba1\u8ba1\u3002";
        }
        summaryArea.setText(summary);
    }

    private boolean isAdmin() {
        return user != null && "ADMIN".equals(user.getRole());
    }

    String accountText() {
        return accountValue.getText();
    }

    String roleText() {
        return roleValue.getText();
    }

    String summaryText() {
        return summaryArea.getText();
    }
}
