package com.scenicticket.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public final class RegisterPanel extends JPanel {
    @FunctionalInterface
    public interface RegistrationAction {
        long register(String username, String password, String email, String phone);
    }

    private final RegistrationAction registrationAction;
    private final Consumer<Long> successAction;
    private final Runnable backAction;
    private final Consumer<String> statusSink;
    private final UiTaskExecutor taskExecutor;

    private final JTextField usernameField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JTextField emailField = new JTextField(22);
    private final JTextField phoneField = new JTextField(22);
    private final JButton registerButton = UiComponents.primaryButton("注册");
    private final JLabel message = new JLabel(" ");

    public RegisterPanel(RegistrationAction registrationAction,
                         Consumer<Long> successAction,
                         Runnable backAction,
                         Consumer<String> statusSink,
                         UiTaskExecutor taskExecutor) {
        super(new GridBagLayout());
        this.registrationAction = Objects.requireNonNull(registrationAction, "registrationAction");
        this.successAction = Objects.requireNonNull(successAction, "successAction");
        this.backAction = Objects.requireNonNull(backAction, "backAction");
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
        addField(fields, 2, "邮箱", emailField);
        addField(fields, 3, "手机号", phoneField);

        JButton backButton = UiComponents.secondaryButton("返回登录");
        registerButton.addActionListener(event -> submit());
        backButton.addActionListener(event -> backAction.run());

        JPanel actions = UiComponents.toolbar();
        actions.add(registerButton);
        actions.add(backButton);
        addWide(fields, 4, actions);
        addWide(fields, 5, message);

        JPanel card = UiComponents.card("用户注册", fields);
        card.setPreferredSize(new Dimension(460, 320));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.CENTER;
        add(card, constraints);
    }

    private void submit() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        char[] passwordChars = passwordField.getPassword();
        String password = new String(passwordChars);
        Arrays.fill(passwordChars, '\0');
        passwordField.setText("");
        message.setForeground(UiTheme.TEXT);
        message.setText("正在注册...");

        taskExecutor.run("用户注册",
                () -> registrationAction.register(username, password, email, phone),
                successAction,
                this::showFailure);
    }

    private void showFailure(String failureMessage) {
        String safeMessage = failureMessage == null || failureMessage.isBlank() ? "注册失败" : failureMessage;
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
        return registerButton;
    }

    void setFormValues(String username, String password, String email, String phone) {
        usernameField.setText(username);
        passwordField.setText(password);
        emailField.setText(email);
        phoneField.setText(phone);
    }

    int passwordLength() {
        return passwordField.getPassword().length;
    }

    String messageText() {
        return message.getText();
    }
}
