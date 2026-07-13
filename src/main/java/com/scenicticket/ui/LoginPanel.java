package com.scenicticket.ui;

import com.scenicticket.dto.LoginResult;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class LoginPanel extends JPanel {
    private final BiFunction<String, String, LoginResult> loginAction;
    private final Consumer<LoginResult> successAction;
    private final Runnable registrationAction;
    private final Consumer<String> statusSink;
    private final UiTaskExecutor taskExecutor;

    private final JTextField usernameField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JButton loginButton = UiComponents.primaryButton("登录");
    private final JLabel message = new JLabel(" ");

    public LoginPanel(BiFunction<String, String, LoginResult> loginAction,
                      Consumer<LoginResult> successAction,
                      Runnable registrationAction,
                      Consumer<String> statusSink,
                      UiTaskExecutor taskExecutor) {
        super(new GridBagLayout());
        this.loginAction = Objects.requireNonNull(loginAction, "loginAction");
        this.successAction = Objects.requireNonNull(successAction, "successAction");
        this.registrationAction = Objects.requireNonNull(registrationAction, "registrationAction");
        this.statusSink = Objects.requireNonNull(statusSink, "statusSink");
        this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor");
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildContent();
    }

    private void buildContent() {
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        addField(fields, 0, "用户名", usernameField);
        addField(fields, 1, "密码", passwordField);

        JButton registerButton = UiComponents.secondaryButton("注册新账号");
        loginButton.addActionListener(event -> submit());
        registerButton.addActionListener(event -> registrationAction.run());

        JPanel actions = UiComponents.toolbar();
        actions.add(loginButton);
        actions.add(registerButton);
        addWide(fields, 2, actions);
        addWide(fields, 3, message);

        JPanel card = UiComponents.card("用户登录", fields);
        card.setPreferredSize(new Dimension(420, 265));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.CENTER;
        add(card, constraints);
    }

    private void submit() {
        String username = usernameField.getText();
        char[] passwordChars = passwordField.getPassword();
        String password = new String(passwordChars);
        Arrays.fill(passwordChars, '\0');
        passwordField.setText("");
        message.setForeground(UiTheme.TEXT);
        message.setText("正在登录...");

        taskExecutor.run("用户登录", () -> loginAction.apply(username, password), result -> {
            if (result == null) {
                showFailure("登录服务未返回结果");
                return;
            }
            message.setText(result.getMessage());
            if (result.isSuccess()) {
                usernameField.setText("");
                successAction.accept(result);
                return;
            }
            showFailure(result.getMessage());
        }, this::showFailure);
    }

    private void showFailure(String failureMessage) {
        String safeMessage = failureMessage == null || failureMessage.isBlank() ? "登录失败" : failureMessage;
        message.setForeground(UiTheme.DANGER);
        message.setText(safeMessage);
        statusSink.accept(safeMessage);
    }

    private void addField(JPanel target, int row, String label, java.awt.Component field) {
        GridBagConstraints labelConstraints = constraints(row, 0);
        target.add(new JLabel(label), labelConstraints);
        GridBagConstraints fieldConstraints = constraints(row, 1);
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1;
        target.add(field, fieldConstraints);
    }

    private void addWide(JPanel target, int row, java.awt.Component component) {
        GridBagConstraints constraints = constraints(row, 1);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        target.add(component, constraints);
    }

    private GridBagConstraints constraints(int row, int column) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }

    JButton defaultButton() {
        return loginButton;
    }

    void setCredentials(String username, String password) {
        usernameField.setText(username);
        passwordField.setText(password);
    }

    String usernameText() {
        return usernameField.getText();
    }

    int passwordLength() {
        return passwordField.getPassword().length;
    }

    String messageText() {
        return message.getText();
    }
}
