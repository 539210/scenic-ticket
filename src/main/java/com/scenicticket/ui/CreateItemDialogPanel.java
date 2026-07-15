package com.scenicticket.ui;

import org.bson.Document;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class CreateItemDialogPanel extends JPanel {
    private final JTextField titleField = new JTextField(24);
    private final JComboBox<CategoryOption> categoryBox = new JComboBox<>();
    private final JTextArea descriptionArea = textArea(5, 24);
    private final JTextField priceField = new JTextField("80.00", 10);
    private final JTextField discountField = new JTextField("0", 10);
    private final ScenicImageDropPanel imagesPanel = new ScenicImageDropPanel();
    private final JTextField openTimeField = new JTextField(18);
    private final JTextField addressField = new JTextField(24);
    private final JTextArea noticeArea = textArea(3, 24);
    private String presetMetadataJson;

    public CreateItemDialogPanel(Map<Long, String> categoryNames) {
        super(new BorderLayout());
        if (categoryNames == null || categoryNames.isEmpty()) {
            throw new IllegalArgumentException("景点类型尚未加载，请先刷新分类");
        }
        categoryNames.forEach((id, name) -> categoryBox.addItem(new CategoryOption(name, id)));
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        addField(fields, 0, "景点名称", titleField);
        addField(fields, 1, "景点类型", categoryBox);
        addField(fields, 2, "景点简介", new JScrollPane(descriptionArea));
        addField(fields, 3, "固定票价", priceField);
        addField(fields, 4, "优惠减免%", discountField);
        addField(fields, 5, "景点图片", imagesPanel);
        addField(fields, 6, "开放时间", openTimeField);
        addField(fields, 7, "景点地址", addressField);
        addField(fields, 8, "游览提示", new JScrollPane(noticeArea));
        add(UiComponents.card("新增景点", fields), BorderLayout.CENTER);
    }

    private static JTextArea textArea(int rows, int columns) {
        JTextArea area = new JTextArea(rows, columns);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
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

    public CreateItemRequest request() {
        CategoryOption category = (CategoryOption) categoryBox.getSelectedItem();
        if (category == null || category.categoryId() == null || category.categoryId() <= 0) {
            throw new IllegalArgumentException("请选择景点类型");
        }
        Document metadata = presetMetadataJson == null
                ? new Document("source", "Swing后台")
                : UiInputParsers.metadataDocument(presetMetadataJson);
        putIfPresent(metadata, "open_time", openTimeField.getText());
        putIfPresent(metadata, "address", addressField.getText());
        putIfPresent(metadata, "notice", noticeArea.getText());
        return new CreateItemRequest(titleField.getText(), category.categoryId(), descriptionArea.getText(),
                imagesPanel.getImageSources(), metadata,
                UiInputParsers.requiredAmount(priceField.getText(), "票价"),
                UiInputParsers.requiredAmount(discountField.getText(), "优惠减免比例"));
    }

    void setFormValues(String title, int categoryIndex, String description, String price,
                       String discount, String images, String metadata) {
        titleField.setText(title);
        categoryBox.setSelectedIndex(categoryIndex);
        descriptionArea.setText(description);
        priceField.setText(price);
        discountField.setText(discount);
        imagesPanel.setImageSources(UiInputParsers.imageLines(images));
        presetMetadataJson = metadata;
        try {
            Document metadataDocument = UiInputParsers.metadataDocument(metadata);
            openTimeField.setText(UiFormatters.readableText(metadataDocument.get("open_time"), ""));
            addressField.setText(UiFormatters.readableText(metadataDocument.get("address"), ""));
            noticeArea.setText(UiFormatters.readableText(metadataDocument.get("notice"), ""));
        } catch (IllegalArgumentException ignored) {
            openTimeField.setText("");
            addressField.setText("");
            noticeArea.setText("");
        }
    }

    private void putIfPresent(Document metadata, String key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.put(key, value.trim());
        }
    }

    public record CreateItemRequest(String title, long categoryId, String description,
                                    List<String> images, Document metadata,
                                    BigDecimal price, BigDecimal discountRate) {
        public CreateItemRequest {
            images = List.copyOf(images);
            metadata = new Document(metadata);
        }
    }
}
