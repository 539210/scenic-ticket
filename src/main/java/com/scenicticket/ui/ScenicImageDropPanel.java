package com.scenicticket.ui;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class ScenicImageDropPanel extends JPanel {
    private final ScenicImageStorage storage;
    private final DefaultListModel<String> displayModel = new DefaultListModel<>();
    private final List<String> imageSources = new ArrayList<>();

    ScenicImageDropPanel() {
        this(new ScenicImageStorage());
    }

    ScenicImageDropPanel(ScenicImageStorage storage) {
        super(new BorderLayout(6, 6));
        this.storage = storage;
        setPreferredSize(new Dimension(420, 120));
        JLabel hint = new JLabel("把图片文件拖到这里（单张不超过 10MB）");
        hint.setHorizontalAlignment(JLabel.CENTER);
        hint.setBorder(BorderFactory.createDashedBorder(new Color(120, 135, 150), 2, 4));
        JList<String> list = new JList<>(displayModel);
        JButton clear = UiComponents.secondaryButton("清空图片");
        clear.addActionListener(event -> setImageSources(List.of()));
        add(hint, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(clear);
        add(actions, BorderLayout.SOUTH);
        setTransferHandler(new FileDropHandler());
        hint.setTransferHandler(getTransferHandler());
        list.setTransferHandler(getTransferHandler());
    }

    List<String> getImageSources() {
        return List.copyOf(imageSources);
    }

    void setImageSources(List<?> sources) {
        imageSources.clear();
        displayModel.clear();
        if (sources == null) {
            return;
        }
        for (Object source : sources) {
            if (source == null || String.valueOf(source).isBlank()) {
                continue;
            }
            String value = String.valueOf(source);
            imageSources.add(value);
            displayModel.addElement(displayName(value));
        }
    }

    private String displayName(String source) {
        try {
            Path path = Path.of(source);
            return path.getFileName() == null ? "已有图片" : path.getFileName().toString();
        } catch (RuntimeException ignored) {
            return "已有图片 " + (displayModel.size() + 1);
        }
    }

    private final class FileDropHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                List<File> files = (List<File>) support.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);
                List<String> combined = new ArrayList<>(imageSources);
                combined.addAll(storage.store(files));
                setImageSources(combined);
                return true;
            } catch (Exception exception) {
                javax.swing.JOptionPane.showMessageDialog(ScenicImageDropPanel.this,
                        UiFormatters.chineseError(exception), "图片导入失败", javax.swing.JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
    }
}
