package com.scenicticket.ui;

import com.scenicticket.model.Profile;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ProfilePanel extends JPanel {
    private final long userId;
    private final Supplier<Optional<Profile>> loader;
    private final Function<Profile, Boolean> saver;
    private final UiTaskExecutor taskExecutor;

    private final JTextField realNameField = new JTextField(24);
    private final JTextField idCardField = new JTextField(24);
    private final JTextField addressField = new JTextField(32);
    private final JTextArea notesArea = new JTextArea(5, 32);
    private final JLabel message = new JLabel(" ");

    public ProfilePanel(long userId, Supplier<Optional<Profile>> loader,
                        Function<Profile, Boolean> saver, UiTaskExecutor taskExecutor) {
        super(new BorderLayout(12, 12));
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        this.userId = userId;
        this.loader = Objects.requireNonNull(loader, "loader");
        this.saver = Objects.requireNonNull(saver, "saver");
        this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor");
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        buildContent();
    }

    private void buildContent() {
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        addField(fields, 0, "\u771f\u5b9e\u59d3\u540d", realNameField);
        addField(fields, 1, "\u8bc1\u4ef6\u53f7", idCardField);
        addField(fields, 2, "\u8054\u7cfb\u5730\u5740", addressField);
        addField(fields, 3, "\u4e2a\u4eba\u7b80\u4ecb", new JScrollPane(notesArea));

        JButton refreshButton = UiComponents.secondaryButton("\u5237\u65b0\u6863\u6848");
        JButton saveButton = UiComponents.primaryButton("\u4fdd\u5b58\u6863\u6848");
        refreshButton.addActionListener(event -> refreshProfile());
        saveButton.addActionListener(event -> saveProfile());

        JPanel actions = UiComponents.toolbar();
        actions.add(refreshButton);
        actions.add(saveButton);
        actions.add(message);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        add(UiComponents.card("\u4e2a\u4eba\u6863\u6848", content), BorderLayout.NORTH);
    }

    private void addField(JPanel panel, int row, String label, java.awt.Component component) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(6, 0, 6, 12);
        panel.add(new JLabel(label), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(6, 0, 6, 0);
        panel.add(component, fieldConstraints);
    }

    private void refreshProfile() {
        taskExecutor.run("\u5237\u65b0\u6863\u6848", loader::get, profile -> {
            if (profile.isPresent()) {
                fillForm(profile.get());
                message.setText("\u6863\u6848\u5df2\u5237\u65b0");
            } else {
                clearForm();
                message.setText("\u6682\u65e0\u6863\u6848\u4fe1\u606f\uff0c\u53ef\u4ee5\u586b\u5199\u540e\u4fdd\u5b58");
            }
        });
    }

    private void saveProfile() {
        taskExecutor.run("\u4fdd\u5b58\u6863\u6848", () -> saver.apply(buildProfile()),
                saved -> message.setText(saved ? "\u6863\u6848\u5df2\u4fdd\u5b58" : "\u6863\u6848\u672a\u66f4\u65b0"));
    }

    private Profile buildProfile() {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setRealName(realNameField.getText());
        profile.setIdCard(idCardField.getText());
        profile.setAddress(addressField.getText());
        profile.setNotes(notesArea.getText());
        return profile;
    }

    private void fillForm(Profile profile) {
        realNameField.setText(text(profile.getRealName()));
        idCardField.setText(text(profile.getIdCard()));
        addressField.setText(text(profile.getAddress()));
        notesArea.setText(text(profile.getNotes()));
    }

    private void clearForm() {
        realNameField.setText("");
        idCardField.setText("");
        addressField.setText("");
        notesArea.setText("");
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    void setFormValues(String realName, String idCard, String address, String notes) {
        realNameField.setText(realName);
        idCardField.setText(idCard);
        addressField.setText(address);
        notesArea.setText(notes);
    }

    String realNameText() {
        return realNameField.getText();
    }

    String messageText() {
        return message.getText();
    }
}
