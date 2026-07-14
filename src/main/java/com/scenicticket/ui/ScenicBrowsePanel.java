package com.scenicticket.ui;

import com.scenicticket.dto.CommentListDTO;
import com.scenicticket.dto.CrossDatabaseItemDTO;
import com.scenicticket.dto.RecommendationDTO;
import com.scenicticket.model.Category;
import com.scenicticket.model.Item;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class ScenicBrowsePanel extends JPanel {
    private static final String ALL_OPTION = "全部";
    private static final String[] KEYWORD_OPTIONS = {
            ALL_OPTION, "南山", "云岭", "青河", "古城", "海湾", "星湖", "观景", "博物馆", "亲子", "水上", "森林"
    };

    private final UiTaskExecutor taskExecutor;
    private final Actions actions;
    private final Function<List<?>, Optional<ImageIcon>> imageLoader;
    private final JComboBox<String> keywordBox = new JComboBox<>(KEYWORD_OPTIONS);
    private final JTextField keywordField = new JTextField(14);
    private final JComboBox<CategoryOption> categoryBox = new JComboBox<>();
    private final JTextArea overviewArea = UiComponents.readOnlyTextArea(18, 34);
    private final JTextArea introductionArea = UiComponents.readOnlyTextArea(18, 34);
    private final JTextArea commentsArea = UiComponents.readOnlyTextArea(18, 34);
    private final JLabel introductionImage = new JLabel();
    private final JPanel introductionImagePanel = new JPanel(new BorderLayout());
    private final DefaultTableModel tableModel = readOnlyTableModel();
    private final JTable table = UiComponents.table(tableModel);
    private final JTabbedPane scenicInfoTabs = new JTabbedPane();
    private final JButton detailButton = UiComponents.secondaryButton("景点简介");
    private final JButton commentsButton = UiComponents.secondaryButton("游客评论");
    private final JButton availabilityButton = UiComponents.secondaryButton("可售票种与日期");
    private final JButton orderButton = UiComponents.primaryButton("购买门票");
    private final JButton commentButton = UiComponents.secondaryButton("发表评论");
    private final List<Item> visibleItems = new ArrayList<>();
    private final Map<Long, String> categoryNames = new LinkedHashMap<>();
    private final Map<Long, String> recommendationReasons = new LinkedHashMap<>();
    private Item selectedItem;

    public ScenicBrowsePanel(UiTaskExecutor taskExecutor, Actions actions) {
        this(taskExecutor, actions, new ScenicImageLoader(360, 220)::loadFirst);
    }

    ScenicBrowsePanel(UiTaskExecutor taskExecutor, Actions actions,
                      Function<List<?>, Optional<ImageIcon>> imageLoader) {
        super(new BorderLayout(12, 12));
        this.taskExecutor = taskExecutor;
        this.actions = actions;
        this.imageLoader = imageLoader;
        setBackground(UiTheme.BACKGROUND);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        configureTable();
        configureDetails();
        add(UiComponents.card("查询景点", createSearchToolbar()), BorderLayout.NORTH);
        add(createSplitPane(), BorderLayout.CENTER);
        loadCategories();
    }

    private JPanel createSearchToolbar() {
        JPanel toolbar = UiComponents.toolbar();
        JButton searchButton = UiComponents.primaryButton("查询");
        JButton recommendButton = UiComponents.secondaryButton("推荐");
        JButton clearButton = UiComponents.secondaryButton("重置");
        JPopupMenu recommendMenu = new JPopupMenu();
        JMenuItem personal = new JMenuItem("为你推荐");
        JMenuItem hot = new JMenuItem("热门");
        JMenuItem rated = new JMenuItem("高分");
        recommendMenu.add(personal);
        recommendMenu.add(hot);
        recommendMenu.add(rated);

        toolbar.add(new JLabel("关键词"));
        toolbar.add(keywordBox);
        toolbar.add(new JLabel("自定义"));
        toolbar.add(keywordField);
        toolbar.add(new JLabel("景点类型"));
        toolbar.add(categoryBox);
        toolbar.add(searchButton);
        toolbar.add(recommendButton);
        toolbar.add(clearButton);

        searchButton.addActionListener(event -> taskExecutor.run("景点查询",
                () -> actions.searchItems(buildSearchKeyword(), selectedCategoryId()), this::fillItems));
        recommendButton.addActionListener(event -> recommendMenu.show(
                recommendButton, 0, recommendButton.getHeight()));
        personal.addActionListener(event -> taskExecutor.run(
                "为你推荐", actions::recommendForUser, this::fillRecommendations));
        hot.addActionListener(event -> taskExecutor.run(
                "热门推荐", actions::recommendHot, this::fillRecommendations));
        rated.addActionListener(event -> taskExecutor.run(
                "高分推荐", actions::recommendTopRated, this::fillRecommendations));
        clearButton.addActionListener(event -> {
            keywordBox.setSelectedItem(ALL_OPTION);
            keywordField.setText("");
            if (categoryBox.getItemCount() > 0) {
                categoryBox.setSelectedIndex(0);
            }
            taskExecutor.run("重置景点列表", () -> actions.searchItems(null, null), this::fillItems);
        });
        keywordField.addActionListener(event -> searchButton.doClick());
        return toolbar;
    }

    private void configureTable() {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int[] widths = {210, 110, 85, 90, 90, 80, 75};
        for (int index = 0; index < widths.length; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || table.getSelectedRow() < 0) {
                return;
            }
            int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
            if (modelRow < 0 || modelRow >= visibleItems.size()) {
                return;
            }
            selectItem(visibleItems.get(modelRow));
        });
    }

    private void configureDetails() {
        overviewArea.setText("请选择查询条件，或点击“查询”浏览全部景点。");
        introductionArea.setText("请选择景点后点击“景点简介”。");
        commentsArea.setText("请选择景点后点击“游客评论”。");
        scenicInfoTabs.addTab("景点概览", UiComponents.scroll(overviewArea));
        JPanel introductionPanel = new JPanel(new BorderLayout(0, 10));
        introductionPanel.setBackground(Color.WHITE);
        introductionPanel.add(UiComponents.scroll(introductionArea), BorderLayout.CENTER);
        introductionImage.setHorizontalAlignment(JLabel.CENTER);
        introductionImage.setVerticalAlignment(JLabel.CENTER);
        introductionImage.getAccessibleContext().setAccessibleName("景点图片");
        introductionImagePanel.setBackground(Color.WHITE);
        introductionImagePanel.setPreferredSize(new Dimension(380, 230));
        introductionImagePanel.add(introductionImage, BorderLayout.CENTER);
        introductionImagePanel.setVisible(false);
        introductionPanel.add(introductionImagePanel, BorderLayout.SOUTH);
        scenicInfoTabs.addTab("景点简介", introductionPanel);
        scenicInfoTabs.addTab("游客评论", UiComponents.scroll(commentsArea));

        setSelectionActionsEnabled(false, false);
        detailButton.addActionListener(event -> loadIntroduction());
        commentsButton.addActionListener(event -> loadComments());
        availabilityButton.addActionListener(event -> actions.showTicketAvailability(requireSelectedItem()));
        orderButton.addActionListener(event -> actions.showPurchase(requireSelectedItem(), overviewArea));
        commentButton.addActionListener(event -> actions.showComment(requireSelectedItem(), commentsArea));
    }

    private JSplitPane createSplitPane() {
        JPanel detailActions = new JPanel(new GridLayout(3, 2, 10, 10));
        detailActions.setOpaque(false);
        detailActions.add(detailButton);
        detailActions.add(commentsButton);
        detailActions.add(availabilityButton);
        detailActions.add(orderButton);
        detailActions.add(commentButton);
        detailActions.add(new JLabel(""));
        JPanel detailPanel = new JPanel(new BorderLayout(0, 10));
        detailPanel.setOpaque(false);
        detailPanel.add(scenicInfoTabs, BorderLayout.CENTER);
        detailPanel.add(detailActions, BorderLayout.SOUTH);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                UiComponents.card("景点列表", UiComponents.scroll(table)),
                UiComponents.card("景点信息", detailPanel));
        splitPane.setResizeWeight(0.64);
        splitPane.setDividerLocation(760);
        return splitPane;
    }

    private void loadCategories() {
        taskExecutor.run("加载景点类型", actions::loadCategories, categories -> {
            actions.categoriesLoaded(categories);
            categoryNames.clear();
            if (categories != null) {
                for (Category category : categories) {
                    categoryNames.put(category.getCategoryId(), category.getName());
                }
            }
            UiCategoryOptions.fill(categoryBox, categories, true, "全部类型");
        });
    }

    private void fillItems(List<Item> items) {
        List<Item> safeItems = items == null ? List.of() : items;
        tableModel.setRowCount(0);
        visibleItems.clear();
        recommendationReasons.clear();
        for (Item item : safeItems) {
            visibleItems.add(item);
            addTableRow(item, "-");
        }
        resetSelection();
        overviewArea.setText(safeItems.isEmpty()
                ? "没有找到符合条件的景点，请调整查询条件。"
                : "共找到 " + safeItems.size() + " 个景点，请从左侧列表选择。");
        actions.setStatus("查询到 " + safeItems.size() + " 个景点");
    }

    void fillRecommendations(List<RecommendationDTO> recommendations) {
        List<RecommendationDTO> safeRecommendations = recommendations == null ? List.of() : recommendations;
        tableModel.setRowCount(0);
        visibleItems.clear();
        recommendationReasons.clear();
        for (RecommendationDTO recommendation : safeRecommendations) {
            Item item = recommendation.getItem();
            if (item != null) {
                visibleItems.add(item);
                recommendationReasons.put(item.getItemId(), recommendation.getReason());
                addTableRow(item, UiFormatters.recommendationScore(recommendation.getScore()));
            }
        }
        resetSelection();
        overviewArea.setText(visibleItems.isEmpty()
                ? "暂时没有推荐结果。"
                : "已生成 " + visibleItems.size() + " 个推荐结果，请选择景点查看推荐理由。");
        actions.setStatus("已生成 " + visibleItems.size() + " 个推荐景点");
    }

    private void addTableRow(Item item, String recommendationScore) {
        tableModel.addRow(new Object[]{
                item.getTitle(), categoryName(item.getCategoryId()), UiFormatters.money(item.getPrice()),
                UiFormatters.discount(item.getDiscountRate()),
                UiFormatters.money(UiFormatters.discountedUnitPrice(item.getPrice(), item.getDiscountRate())),
                recommendationScore, UiFormatters.itemStatus(item.getStatus())
        });
    }

    private void selectItem(Item item) {
        selectedItem = item;
        String reason = recommendationReasons.get(item.getItemId());
        overviewArea.setText(item.getTitle() + System.lineSeparator()
                + "类型：" + categoryName(item.getCategoryId()) + System.lineSeparator()
                + "原价：" + UiFormatters.money(item.getPrice()) + "    优惠："
                + UiFormatters.discount(item.getDiscountRate()) + System.lineSeparator()
                + "折后价：" + UiFormatters.money(UiFormatters.discountedUnitPrice(
                item.getPrice(), item.getDiscountRate())) + System.lineSeparator()
                + "状态：" + UiFormatters.itemStatus(item.getStatus())
                + (reason == null ? "" : System.lineSeparator() + "推荐理由：" + reason)
                + System.lineSeparator() + System.lineSeparator()
                + "点击“景点简介”查看景区介绍，点击“游客评论”查看评价。");
        introductionArea.setText("尚未加载“" + item.getTitle() + "”的景点简介。");
        hideIntroductionImage();
        commentsArea.setText("尚未加载“" + item.getTitle() + "”的游客评论。");
        scenicInfoTabs.setSelectedIndex(0);
        boolean available = item.getStatus() != null && item.getStatus() == 1;
        setSelectionActionsEnabled(true, available);
    }

    private void loadIntroduction() {
        long itemId = requireSelectedItem().getItemId();
        taskExecutor.run("景点详情", () -> actions.loadItemDetail(itemId), dto -> {
            if (isSelectedItem(itemId)) {
                introductionArea.setText(actions.formatItemIntroduction(dto));
                introductionArea.setCaretPosition(0);
                loadIntroductionImage(itemId, dto);
                scenicInfoTabs.setSelectedIndex(1);
            }
        });
    }

    private void loadComments() {
        Item requestedItem = requireSelectedItem();
        long itemId = requestedItem.getItemId();
        taskExecutor.run("游客评论", () -> actions.loadComments(itemId), dto -> {
            if (isSelectedItem(itemId)) {
                commentsArea.setText(actions.formatCommentViews(requestedItem.getTitle(), dto));
                scenicInfoTabs.setSelectedIndex(2);
            }
        });
    }

    private void resetSelection() {
        table.clearSelection();
        selectedItem = null;
        setSelectionActionsEnabled(false, false);
        introductionArea.setText("请选择景点后点击“景点简介”。");
        hideIntroductionImage();
        commentsArea.setText("请选择景点后点击“游客评论”。");
    }

    private void setSelectionActionsEnabled(boolean selected, boolean available) {
        detailButton.setEnabled(selected);
        commentsButton.setEnabled(selected);
        availabilityButton.setEnabled(selected && available);
        orderButton.setEnabled(selected && available);
        commentButton.setEnabled(selected);
    }

    private Item requireSelectedItem() {
        if (selectedItem == null) {
            throw new IllegalArgumentException("请先从表格中选择一个景点");
        }
        return selectedItem;
    }

    private boolean isSelectedItem(long itemId) {
        return selectedItem != null && selectedItem.getItemId() != null && selectedItem.getItemId() == itemId;
    }

    private void loadIntroductionImage(long itemId, CrossDatabaseItemDTO dto) {
        hideIntroductionImage();
        List<?> imageSources = imageSources(dto);
        if (imageSources.isEmpty()) {
            return;
        }
        taskExecutor.run("景点图片", () -> imageLoader.apply(imageSources), image -> {
            if (!isSelectedItem(itemId)) {
                return;
            }
            if (image != null && image.isPresent()) {
                introductionImage.setIcon(image.get());
                introductionImagePanel.setVisible(true);
                introductionImagePanel.revalidate();
                introductionImagePanel.repaint();
            } else {
                hideIntroductionImage();
            }
        });
    }

    private List<?> imageSources(CrossDatabaseItemDTO dto) {
        if (dto == null || dto.getDetail() == null) {
            return List.of();
        }
        Object value = dto.getDetail().get("images");
        if (value instanceof List<?> images) {
            return images;
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return List.of();
        }
        return List.of(value);
    }

    private void hideIntroductionImage() {
        introductionImage.setIcon(null);
        introductionImagePanel.setVisible(false);
        introductionImagePanel.revalidate();
        introductionImagePanel.repaint();
    }

    private Long selectedCategoryId() {
        Object selected = categoryBox.getSelectedItem();
        return selected instanceof CategoryOption option ? option.categoryId() : null;
    }

    private String buildSearchKeyword() {
        String preset = keywordBox.getSelectedItem() == null || ALL_OPTION.equals(keywordBox.getSelectedItem())
                ? "" : String.valueOf(keywordBox.getSelectedItem()).trim();
        String custom = keywordField.getText() == null ? "" : keywordField.getText().trim();
        String combined = (preset + " " + custom).trim();
        return combined.isEmpty() ? null : combined;
    }

    private String categoryName(Long categoryId) {
        return categoryId == null ? "未分类" : categoryNames.getOrDefault(categoryId, "类型 #" + categoryId);
    }

    private static DefaultTableModel readOnlyTableModel() {
        return new DefaultTableModel(
                new Object[]{"景点名称", "类型", "原价", "优惠", "折后价", "推荐分", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    int itemRowCount() {
        return tableModel.getRowCount();
    }

    Object tableValueAt(int row, int column) {
        return tableModel.getValueAt(row, column);
    }

    void selectRow(int row) {
        table.setRowSelectionInterval(row, row);
    }

    String overviewText() {
        return overviewArea.getText();
    }

    boolean purchaseEnabled() {
        return orderButton.isEnabled();
    }

    boolean introductionImageVisible() {
        return introductionImagePanel.isVisible() && introductionImage.getIcon() != null;
    }

    public interface Actions {
        List<Category> loadCategories();

        void categoriesLoaded(List<Category> categories);

        List<Item> searchItems(String keyword, Long categoryId);

        List<RecommendationDTO> recommendForUser();

        List<RecommendationDTO> recommendTopRated();

        List<RecommendationDTO> recommendHot();

        CrossDatabaseItemDTO loadItemDetail(long itemId);

        CommentListDTO loadComments(long itemId);

        String formatItemIntroduction(CrossDatabaseItemDTO dto);

        String formatCommentViews(String itemTitle, CommentListDTO dto);

        void showTicketAvailability(Item item);

        void showPurchase(Item item, JTextArea detailArea);

        void showComment(Item item, JTextArea commentsArea);

        void setStatus(String message);
    }
}
