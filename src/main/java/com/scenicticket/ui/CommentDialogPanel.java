package com.scenicticket.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;

public final class CommentDialogPanel extends JPanel {
    private final JSpinner ratingSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
    private final JTextArea commentArea = new JTextArea(5, 28);
    private final JTextField tagsField = new JTextField(28);

    public CommentDialogPanel(String itemTitle) {
        super(new BorderLayout());
        if (itemTitle == null || itemTitle.isBlank()) {
            throw new IllegalArgumentException("景点名称不能为空");
        }
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        addField(fields, 0, "景点", new JLabel(itemTitle));
        addField(fields, 1, "评分", ratingSpinner);
        addField(fields, 2, "评论内容", new JScrollPane(commentArea));
        addField(fields, 3, "标签（逗号分隔）", tagsField);
        add(UiComponents.card("发表评论", fields), BorderLayout.CENTER);
    }

    private void addField(JPanel target, int row, String label, java.awt.Component field) {
        GridBagConstraints labelConstraints = constraints(row, 0);
        target.add(new JLabel(label), labelConstraints);
        GridBagConstraints fieldConstraints = constraints(row, 1);
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1;
        target.add(field, fieldConstraints);
    }

    private GridBagConstraints constraints(int row, int column) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        return constraints;
    }

    public CommentSubmission submission() {
        return new CommentSubmission(commentArea.getText(), (Integer) ratingSpinner.getValue(), parseTags());
    }

    private List<String> parseTags() {
        String text = tagsField.getText();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("[,，]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    void setFormValues(String content, int rating, String tags) {
        commentArea.setText(content);
        ratingSpinner.setValue(rating);
        tagsField.setText(tags);
    }

    public record CommentSubmission(String content, int rating, List<String> tags) {
        public CommentSubmission {
            tags = List.copyOf(tags);
        }
    }
}
