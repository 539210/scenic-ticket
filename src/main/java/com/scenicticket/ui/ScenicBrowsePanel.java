package com.scenicticket.ui;

import com.scenicticket.dto.CommentListDTO;
import com.scenicticket.dto.CommentViewDTO;
import com.scenicticket.dto.CrossDatabaseItemDTO;
import com.scenicticket.dto.RecommendationDTO;
import com.scenicticket.model.Category;
import com.scenicticket.model.Item;

import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final JLabel commentsSummary = new JLabel("请选择景点查看游客评分与评论");
    private final JPanel commentsListPanel = new JPanel();
    private final JScrollPane commentsScroll = UiComponents.scroll(commentsListPanel);
    private final JLabel introductionImage = new JLabel();
    private final JPanel introductionImagePanel = new JPanel(new BorderLayout());
    private final DefaultTableModel tableModel = readOnlyTableModel();
    private final JTable table = UiComponents.table(tableModel);
    private final JTabbedPane scenicInfoTabs = new JTabbedPane();
    private final List<JButton> scenicInfoIndicators = new ArrayList<>();
    private final JButton availabilityButton = UiComponents.secondaryButton("可售票种与日期");
    private final JButton orderButton = UiComponents.primaryButton("购买门票");
    private final JButton commentButton = UiComponents.secondaryButton("发表评论");
    private final List<Item> visibleItems = new ArrayList<>();
    private final Map<Long, String> categoryNames = new LinkedHashMap<>();
    private final Map<Long, String> recommendationReasons = new LinkedHashMap<>();
    private final Map<Long, CrossDatabaseItemDTO> detailCache = new LinkedHashMap<>();
    private final Map<Long, CommentListDTO> commentCache = new LinkedHashMap<>();
    private final Map<Long, Optional<ImageIcon>> imageCache = new LinkedHashMap<>();
    private final Set<Long> loadingDetails = new HashSet<>();
    private final Set<Long> loadingComments = new HashSet<>();
    private int contentGeneration;
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
        recommendButton.setToolTipText("按游客评分从高到低推荐");
        recommendButton.addActionListener(event -> taskExecutor.run(
                "评分推荐", actions::recommendTopRated, this::fillRecommendations));
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
        int[] widths = {220, 120, 90, 90, 95, 100};
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
        introductionArea.setText("请选择景点后打开“景点简介”。");
        scenicInfoTabs.addTab("景点概览", UiComponents.scroll(overviewArea));
        JPanel introductionPanel = new JPanel(new BorderLayout(0, 10));
        introductionPanel.setBackground(Color.WHITE);
        introductionPanel.add(UiComponents.scroll(introductionArea), BorderLayout.CENTER);
        introductionImage.setHorizontalAlignment(JLabel.CENTER);
        introductionImage.setVerticalAlignment(JLabel.CENTER);
        introductionImage.getAccessibleContext().setAccessibleName("景点图片");
        introductionImagePanel.setBackground(Color.WHITE);
        introductionImagePanel.setPreferredSize(new Dimension(320, 160));
        introductionImagePanel.add(introductionImage, BorderLayout.CENTER);
        introductionImagePanel.setVisible(false);
        introductionPanel.add(introductionImagePanel, BorderLayout.SOUTH);
        scenicInfoTabs.addTab("景点简介", introductionPanel);
        scenicInfoTabs.addTab("游客评论", createCommentsPanel());
        configureInfoIndicators();

        setSelectionActionsEnabled(false, false);
        availabilityButton.addActionListener(event -> actions.showTicketAvailability(requireSelectedItem()));
        orderButton.addActionListener(event -> actions.showPurchase(requireSelectedItem(), overviewArea));
        commentButton.addActionListener(event -> actions.showComment(requireSelectedItem(), this::refreshComments));
    }

    private JPanel createCommentsPanel() {
        commentsListPanel.setLayout(new BoxLayout(commentsListPanel, BoxLayout.Y_AXIS));
        commentsListPanel.setBackground(Color.WHITE);
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(Color.WHITE);
        commentsSummary.setFont(UiTheme.SECTION_FONT);
        header.add(commentsSummary, BorderLayout.CENTER);
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionBar.setOpaque(false);
        actionBar.add(commentButton);
        header.add(actionBar, BorderLayout.EAST);
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(header, BorderLayout.NORTH);
        panel.add(commentsScroll, BorderLayout.CENTER);
        return panel;
    }

    private void configureInfoIndicators() {
        scenicInfoIndicators.add(UiComponents.secondaryButton("景点概览"));
        scenicInfoIndicators.add(UiComponents.secondaryButton("景点简介"));
        scenicInfoIndicators.add(UiComponents.secondaryButton("游客评论"));
        for (int index = 0; index < scenicInfoTabs.getTabCount(); index += 1) {
            JButton indicator = scenicInfoIndicators.get(index);
            int tabIndex = index;
            indicator.addActionListener(event -> showInfoTab(tabIndex));
            indicator.getAccessibleContext().setAccessibleDescription("点击切换到" + scenicInfoTabs.getTitleAt(index));
            scenicInfoTabs.setTabComponentAt(index, indicator);
            scenicInfoTabs.setEnabledAt(index, true);
        }
        scenicInfoTabs.addChangeListener(event -> {
            int selectedIndex = scenicInfoTabs.getSelectedIndex();
            updateInfoIndicatorStyles(selectedIndex);
            if (selectedItem == null) return;
            if (selectedIndex == 1) ensureIntroduction(selectedItem.getItemId());
            if (selectedIndex == 2) ensureComments(selectedItem);
        });
        showInfoTab(0);
    }

    private void showInfoTab(int index) {
        scenicInfoTabs.setSelectedIndex(index);
        updateInfoIndicatorStyles(index);
    }

    private void updateInfoIndicatorStyles(int index) {
        for (int tab = 0; tab < scenicInfoIndicators.size(); tab += 1) {
            UiComponents.setSelectedStyle(scenicInfoIndicators.get(tab), tab == index);
        }
    }

    private JSplitPane createSplitPane() {
        JPanel detailActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        detailActions.setOpaque(false);
        detailActions.add(availabilityButton);
        detailActions.add(orderButton);
        JPanel detailPanel = new JPanel(new BorderLayout(0, 10));
        detailPanel.setOpaque(false);
        detailPanel.add(scenicInfoTabs, BorderLayout.CENTER);
        detailPanel.add(detailActions, BorderLayout.SOUTH);
        JPanel listCard = UiComponents.card("景点列表", UiComponents.scroll(table));
        JPanel detailCard = UiComponents.card("景点信息", detailPanel);
        listCard.setMinimumSize(new Dimension(360, 260));
        detailCard.setMinimumSize(new Dimension(300, 260));
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listCard, detailCard);
        splitPane.setResizeWeight(0.64);
        splitPane.setDividerLocation(0.64);
        splitPane.setContinuousLayout(true);
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
        clearDetailCaches();
        for (Item item : safeItems) {
            visibleItems.add(item);
            addTableRow(item, "暂无评分");
        }
        resetSelection();
        overviewArea.setText(safeItems.isEmpty()
                ? "没有找到符合条件的景点，请调整查询条件。"
                : "共找到 " + safeItems.size() + " 个景点，请从左侧列表选择。");
        actions.setStatus("查询到 " + safeItems.size() + " 个景点");
        loadRatings(safeItems);
    }

    private void loadRatings(List<Item> items) {
        List<Long> itemIds = items.stream().map(Item::getItemId).filter(java.util.Objects::nonNull).toList();
        if (itemIds.isEmpty()) {
            return;
        }
        taskExecutor.run("加载游客评分", () -> actions.loadRatings(itemIds), ratings -> {
            List<Long> currentItemIds = visibleItems.stream().map(Item::getItemId)
                    .filter(java.util.Objects::nonNull).toList();
            if (!itemIds.equals(currentItemIds)) {
                return;
            }
            for (int row = 0; row < visibleItems.size(); row += 1) {
                Item item = visibleItems.get(row);
                if (row < tableModel.getRowCount() && item.getItemId() != null) {
                    tableModel.setValueAt(UiFormatters.ratingScore(
                            ratings == null ? null : ratings.get(item.getItemId())), row, 5);
                }
            }
        });
    }

    void fillRecommendations(List<RecommendationDTO> recommendations) {
        List<RecommendationDTO> safeRecommendations = recommendations == null ? List.of() : recommendations;
        tableModel.setRowCount(0);
        visibleItems.clear();
        recommendationReasons.clear();
        clearDetailCaches();
        for (RecommendationDTO recommendation : safeRecommendations) {
            Item item = recommendation.getItem();
            if (item != null) {
                visibleItems.add(item);
                recommendationReasons.put(item.getItemId(), recommendation.getReason());
                addTableRow(item, UiFormatters.ratingScore(recommendation.getScore()));
            }
        }
        resetSelection();
        overviewArea.setText(visibleItems.isEmpty()
                ? "暂时没有推荐结果。"
                : "已生成 " + visibleItems.size() + " 个推荐结果，请选择景点查看推荐理由。");
        actions.setStatus("已生成 " + visibleItems.size() + " 个推荐景点");
    }

    private void addTableRow(Item item, String ratingScore) {
        tableModel.addRow(new Object[]{
                item.getTitle(), categoryName(item.getCategoryId()), UiFormatters.money(item.getPrice()),
                UiFormatters.discount(item.getDiscountRate()),
                UiFormatters.money(UiFormatters.discountedUnitPrice(item.getPrice(), item.getDiscountRate())),
                ratingScore
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
                item.getPrice(), item.getDiscountRate()))
                + (reason == null ? "" : System.lineSeparator() + "推荐理由：" + reason)
                + System.lineSeparator() + System.lineSeparator()
                + "使用上方页签查看景区简介和游客评论。");
        introductionArea.setText("尚未加载“" + item.getTitle() + "”的景点简介。");
        hideIntroductionImage();
        commentsSummary.setText(item.getTitle() + "　正在加载评分…");
        showCommentsPlaceholder("正在预取“" + item.getTitle() + "”的游客评论…");
        showInfoTab(0);
        boolean available = item.getStatus() != null && item.getStatus() == 1;
        setSelectionActionsEnabled(true, available);
        requestIntroduction(item.getItemId());
        requestComments(item);
    }

    private void ensureIntroduction(long itemId) {
        if (detailCache.containsKey(itemId)) {
            renderIntroduction(itemId, detailCache.get(itemId));
            return;
        }
        introductionArea.setText("正在加载景点简介，请稍候…");
        requestIntroduction(itemId);
    }

    private void requestIntroduction(long itemId) {
        if (detailCache.containsKey(itemId) || !loadingDetails.add(itemId)) return;
        int generation = contentGeneration;
        taskExecutor.runQuietly("景点详情预取#" + itemId, () -> actions.loadItemDetail(itemId), dto -> {
            loadingDetails.remove(itemId);
            if (generation != contentGeneration) return;
            detailCache.put(itemId, dto);
            if (isSelectedItem(itemId) && scenicInfoTabs.getSelectedIndex() == 1) renderIntroduction(itemId, dto);
        }, error -> {
            loadingDetails.remove(itemId);
            if (generation == contentGeneration && isSelectedItem(itemId)
                    && scenicInfoTabs.getSelectedIndex() == 1) {
                introductionArea.setText("景点简介加载失败，切换页签可重试：" + error);
            }
        });
    }

    private void renderIntroduction(long itemId, CrossDatabaseItemDTO dto) {
        if (dto == null) {
            introductionArea.setText("管理员暂未填写该景点简介。");
            hideIntroductionImage();
            return;
        }
        introductionArea.setText(actions.formatItemIntroduction(dto));
        introductionArea.setCaretPosition(0);
        loadIntroductionImage(itemId, dto);
    }

    private void ensureComments(Item item) {
        if (commentCache.containsKey(item.getItemId())) {
            renderComments(item.getTitle(), commentCache.get(item.getItemId()));
            return;
        }
        showCommentsPlaceholder("正在加载游客评论，请稍候…");
        requestComments(item);
    }

    private void requestComments(Item requestedItem) {
        long itemId = requestedItem.getItemId();
        if (commentCache.containsKey(itemId) || !loadingComments.add(itemId)) return;
        int generation = contentGeneration;
        taskExecutor.runQuietly("游客评论预取#" + itemId, () -> actions.loadComments(itemId), dto -> {
            loadingComments.remove(itemId);
            if (generation != contentGeneration) return;
            commentCache.put(itemId, dto);
            if (isSelectedItem(itemId) && scenicInfoTabs.getSelectedIndex() == 2) {
                renderComments(requestedItem.getTitle(), dto);
            }
        }, error -> {
            loadingComments.remove(itemId);
            if (generation == contentGeneration && isSelectedItem(itemId)
                    && scenicInfoTabs.getSelectedIndex() == 2) {
                showCommentsPlaceholder("游客评论加载失败，切换页签可重试：" + error);
            }
        });
    }

    private void refreshComments() {
        Item item = requireSelectedItem();
        commentCache.remove(item.getItemId());
        showInfoTab(2);
        ensureComments(item);
    }

    private void renderComments(String itemTitle, CommentListDTO dto) {
        commentsListPanel.removeAll();
        org.bson.Document summary = dto == null ? null : dto.ratingSummary();
        commentsSummary.setText(itemTitle + "　" + ratingSummaryText(summary));
        List<CommentViewDTO> comments = dto == null || dto.comments() == null ? List.of() : dto.comments();
        if (comments.isEmpty()) {
            showCommentsPlaceholder("暂无评论。购买并支付该景点门票后可发表第一条评论。");
            return;
        }
        for (CommentViewDTO comment : comments) {
            JPanel card = new JPanel(new BorderLayout(0, 8));
            card.setBackground(new Color(248, 250, 252));
            card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(UiTheme.BORDER),
                    javax.swing.BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            int rating = Math.max(0, Math.min(5, comment.rating()));
            JLabel title = new JLabel(comment.displayUsername() + "　" + "★".repeat(rating)
                    + "☆".repeat(5 - rating));
            title.setFont(UiTheme.SECTION_FONT);
            JTextArea content = UiComponents.readOnlyTextArea(2, 24);
            content.setText(UiFormatters.readableText(comment.content(), "该评论没有文字内容"));
            JLabel meta = new JLabel("评论时间：" + UiFormatters.date(comment.createdAt()));
            meta.setForeground(UiTheme.MUTED);
            card.add(title, BorderLayout.NORTH);
            card.add(content, BorderLayout.CENTER);
            card.add(meta, BorderLayout.SOUTH);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
            card.setAlignmentX(LEFT_ALIGNMENT);
            commentsListPanel.add(card);
            commentsListPanel.add(Box.createVerticalStrut(8));
        }
        commentsListPanel.revalidate();
        commentsListPanel.repaint();
        javax.swing.SwingUtilities.invokeLater(() -> commentsScroll.getVerticalScrollBar().setValue(0));
    }

    private void showCommentsPlaceholder(String text) {
        commentsListPanel.removeAll();
        JLabel placeholder = new JLabel(text);
        placeholder.setForeground(UiTheme.MUTED);
        placeholder.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 8, 8, 8));
        commentsListPanel.add(placeholder);
        commentsListPanel.revalidate();
        commentsListPanel.repaint();
    }

    private String ratingSummaryText(org.bson.Document summary) {
        if (summary == null || summary.isEmpty()) return "暂无评分";
        Object count = summary.get("comment_count");
        Object average = summary.get("avg_rating");
        String averageText = average instanceof Number number
                ? String.format(java.util.Locale.ROOT, "%.1f", number.doubleValue()) : "-";
        return "平均 " + averageText + " / 5　共 " + (count == null ? 0 : count) + " 条评论";
    }

    private void resetSelection() {
        table.clearSelection();
        selectedItem = null;
        setSelectionActionsEnabled(false, false);
        introductionArea.setText("请选择景点后点击“景点简介”。");
        hideIntroductionImage();
        commentsSummary.setText("请选择景点查看游客评分与评论");
        showCommentsPlaceholder("请选择左侧景点");
        showInfoTab(0);
    }

    private void setSelectionActionsEnabled(boolean selected, boolean available) {
        for (int index = 1; index < scenicInfoIndicators.size(); index++) {
            scenicInfoIndicators.get(index).setEnabled(selected);
        }
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
        if (imageCache.containsKey(itemId)) {
            showIntroductionImage(imageCache.get(itemId));
            return;
        }
        List<?> imageSources = imageSources(dto);
        if (imageSources.isEmpty()) {
            imageCache.put(itemId, Optional.empty());
            return;
        }
        taskExecutor.run("景点图片", () -> imageLoader.apply(imageSources), image -> {
            Optional<ImageIcon> safeImage = image == null ? Optional.empty() : image;
            imageCache.put(itemId, safeImage);
            if (!isSelectedItem(itemId)) {
                return;
            }
            showIntroductionImage(safeImage);
        });
    }

    private void showIntroductionImage(Optional<ImageIcon> image) {
        if (image != null && image.isPresent()) {
            introductionImage.setIcon(image.get());
            introductionImagePanel.setVisible(true);
            introductionImagePanel.revalidate();
            introductionImagePanel.repaint();
        } else {
            hideIntroductionImage();
        }
    }

    private void clearDetailCaches() {
        contentGeneration++;
        detailCache.clear();
        commentCache.clear();
        imageCache.clear();
        loadingDetails.clear();
        loadingComments.clear();
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
                new Object[]{"景点名称", "类型", "原价", "优惠", "折后价", "游客评分"}, 0) {
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

    int selectedInfoTab() {
        return scenicInfoTabs.getSelectedIndex();
    }

    boolean infoTabClickable(int index) {
        return scenicInfoTabs.isEnabledAt(index);
    }

    boolean infoIndicatorSelected(int index) {
        return UiComponents.isSelectedStyle(scenicInfoIndicators.get(index));
    }

    public interface Actions {
        List<Category> loadCategories();

        void categoriesLoaded(List<Category> categories);

        List<Item> searchItems(String keyword, Long categoryId);

        List<RecommendationDTO> recommendForUser();

        List<RecommendationDTO> recommendTopRated();

        List<RecommendationDTO> recommendHot();

        default Map<Long, Double> loadRatings(List<Long> itemIds) {
            return Map.of();
        }

        CrossDatabaseItemDTO loadItemDetail(long itemId);

        CommentListDTO loadComments(long itemId);

        String formatItemIntroduction(CrossDatabaseItemDTO dto);

        String formatCommentViews(String itemTitle, CommentListDTO dto);

        void showTicketAvailability(Item item);

        void showPurchase(Item item, JTextArea detailArea);

        void showComment(Item item, Runnable refreshComments);

        void setStatus(String message);
    }
}
