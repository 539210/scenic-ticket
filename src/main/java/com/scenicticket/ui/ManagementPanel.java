package com.scenicticket.ui;

import com.scenicticket.model.Category;
import com.scenicticket.model.Item;
import org.bson.Document;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ManagementPanel extends JPanel {
    private final UiTaskExecutor taskExecutor;
    private final Actions actions;
    private final Map<Long, String> categoryNames = new LinkedHashMap<>();

    private final JTextField itemKeywordField = new JTextField(16);
    private final JComboBox<CategoryOption> itemCategoryBox = new JComboBox<>();
    private final DefaultTableModel itemModel = itemModel();
    private final JTable itemTable = UiComponents.table(itemModel);
    private final List<Item> visibleItems = new ArrayList<>();
    private Item selectedItem;
    private final JLabel selectedItemLabel = new JLabel("请先从表格选择景点");
    private final JTextField itemTitleField = new JTextField(14);
    private final JComboBox<CategoryOption> itemEditCategoryBox = new JComboBox<>();
    private final JTextField itemPriceField = new JTextField(8);
    private final JTextField itemDiscountField = new JTextField(6);
    private final JComboBox<String> itemStatusBox = new JComboBox<>(new String[]{"下架", "上架"});
    private final JButton basicItemButton = UiComponents.primaryButton("更新名称和分类");
    private final JButton pricingButton = UiComponents.primaryButton("更新票价和优惠");
    private final JButton itemStatusButton = UiComponents.secondaryButton("更新上下架");
    private final JTextArea itemIntroArea = textArea(4, 48);
    private final JTextArea itemImagesArea = textArea(3, 48);
    private final JTextArea itemMetadataArea = textArea(3, 48);
    private final JButton updateIntroButton = UiComponents.primaryButton("更新完整详情");

    private final DefaultTableModel categoryModel = categoryModel();
    private final JTable categoryTable = UiComponents.table(categoryModel);
    private final List<Category> visibleCategories = new ArrayList<>();
    private Category selectedCategory;
    private final JTextField categoryNameField = new JTextField(16);
    private final JComboBox<CategoryOption> parentCategoryBox = new JComboBox<>();
    private final JButton updateCategoryButton = UiComponents.secondaryButton("更新所选分类");

    public ManagementPanel(UiTaskExecutor taskExecutor, Actions actions,
                           JPanel userPanel, JPanel inventoryPanel, JPanel admissionPanel) {
        super(new BorderLayout(12, 12));
        this.taskExecutor = taskExecutor;
        this.actions = actions;
        setBackground(UiTheme.BACKGROUND);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setItemActionsEnabled(false);
        updateCategoryButton.setEnabled(false);
        configureItemSelection();
        configureCategorySelection();
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.addTab("景点管理", createItemPage());
        tabs.addTab("分类管理", createCategoryPage());
        tabs.addTab("用户管理", userPanel);
        tabs.addTab("票种与库存", inventoryPanel);
        tabs.addTab("门票核销", admissionPanel);
        add(tabs, BorderLayout.CENTER);
        loadCategories(null);
    }

    private JPanel createItemPage() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setOpaque(false);
        JPanel queryToolbar = UiComponents.toolbar();
        JButton queryButton = UiComponents.primaryButton("查询");
        JButton refreshButton = UiComponents.secondaryButton("刷新");
        JButton resetButton = UiComponents.secondaryButton("重置");
        JButton createButton = UiComponents.primaryButton("新增景点");
        queryToolbar.add(new JLabel("关键词"));
        queryToolbar.add(itemKeywordField);
        queryToolbar.add(new JLabel("景点类型"));
        queryToolbar.add(itemCategoryBox);
        queryToolbar.add(queryButton);
        queryToolbar.add(refreshButton);
        queryToolbar.add(resetButton);
        queryToolbar.add(createButton);
        queryButton.addActionListener(event -> loadItems(itemFilterSnapshot(), null));
        refreshButton.addActionListener(event -> loadItems(itemFilterSnapshot(), null));
        itemKeywordField.addActionListener(event -> loadItems(itemFilterSnapshot(), null));
        resetButton.addActionListener(event -> {
            itemKeywordField.setText("");
            if (itemCategoryBox.getItemCount() > 0) {
                itemCategoryBox.setSelectedIndex(0);
            }
            loadItems(itemFilterSnapshot(), null);
        });
        createButton.addActionListener(event -> actions.showCreateItemDialog(
                () -> loadItems(itemFilterSnapshot(), null)));

        JPanel editToolbar = UiComponents.toolbar();
        editToolbar.add(selectedItemLabel);
        editToolbar.add(new JLabel("名称"));
        editToolbar.add(itemTitleField);
        editToolbar.add(new JLabel("分类"));
        editToolbar.add(itemEditCategoryBox);
        editToolbar.add(basicItemButton);
        editToolbar.add(new JLabel("门票原价"));
        editToolbar.add(itemPriceField);
        JLabel discountLabel = new JLabel("优惠减免%");
        discountLabel.setToolTipText("例如填写20表示减免20%，即按原价的80%售票");
        editToolbar.add(discountLabel);
        editToolbar.add(itemDiscountField);
        editToolbar.add(pricingButton);
        editToolbar.add(new JLabel("状态"));
        editToolbar.add(itemStatusBox);
        editToolbar.add(itemStatusButton);
        basicItemButton.addActionListener(event -> updateItemBasic());
        pricingButton.addActionListener(event -> updateItemPricing());
        itemStatusButton.addActionListener(event -> updateItemStatus());

        JTabbedPane detailTabs = new JTabbedPane();
        detailTabs.addTab("景点简介", new JScrollPane(itemIntroArea));
        detailTabs.addTab("图片地址（每行一个）", new JScrollPane(itemImagesArea));
        detailTabs.addTab("扩展属性（JSON）", new JScrollPane(itemMetadataArea));
        JPanel detailEditor = new JPanel(new BorderLayout(10, 0));
        detailEditor.setOpaque(false);
        detailEditor.add(detailTabs, BorderLayout.CENTER);
        detailEditor.add(updateIntroButton, BorderLayout.EAST);
        updateIntroButton.addActionListener(event -> updateItemDetail());

        JPanel top = new JPanel(new GridLayout(3, 1, 0, 8));
        top.setOpaque(false);
        top.add(UiComponents.card("筛选景点", queryToolbar));
        top.add(UiComponents.card("编辑所选景点", editToolbar));
        top.add(UiComponents.card("编辑景点简介", detailEditor));
        page.add(top, BorderLayout.NORTH);
        page.add(UiComponents.card("景点列表", UiComponents.scroll(itemTable)), BorderLayout.CENTER);
        return page;
    }

    private JPanel createCategoryPage() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setOpaque(false);
        JPanel toolbar = UiComponents.toolbar();
        JButton createButton = UiComponents.primaryButton("新增分类");
        JButton refreshButton = UiComponents.secondaryButton("刷新");
        toolbar.add(new JLabel("分类名称"));
        toolbar.add(categoryNameField);
        toolbar.add(new JLabel("上级分类"));
        toolbar.add(parentCategoryBox);
        toolbar.add(createButton);
        toolbar.add(updateCategoryButton);
        toolbar.add(refreshButton);
        refreshButton.addActionListener(event -> loadCategories(null));
        createButton.addActionListener(event -> createCategory());
        updateCategoryButton.addActionListener(event -> updateCategory());
        categoryNameField.addActionListener(event -> createButton.doClick());
        page.add(UiComponents.card("新增或编辑分类", toolbar), BorderLayout.NORTH);
        page.add(UiComponents.card("分类列表", UiComponents.scroll(categoryTable)), BorderLayout.CENTER);
        return page;
    }

    private void configureItemSelection() {
        itemTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || itemTable.getSelectedRow() < 0) {
                return;
            }
            int modelRow = itemTable.convertRowIndexToModel(itemTable.getSelectedRow());
            if (modelRow < 0 || modelRow >= visibleItems.size()) {
                return;
            }
            selectItem(visibleItems.get(modelRow));
        });
    }

    private void configureCategorySelection() {
        categoryTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || categoryTable.getSelectedRow() < 0) {
                return;
            }
            int modelRow = categoryTable.convertRowIndexToModel(categoryTable.getSelectedRow());
            if (modelRow < 0 || modelRow >= visibleCategories.size()) {
                return;
            }
            selectedCategory = visibleCategories.get(modelRow);
            categoryNameField.setText(selectedCategory.getName());
            selectCategory(parentCategoryBox, selectedCategory.getParentId());
            updateCategoryButton.setEnabled(true);
        });
    }

    private void loadCategories(String completionMessage) {
        taskExecutor.run("刷新分类", actions::listCategories, categories -> {
            List<Category> safeCategories = categories == null ? List.of() : categories;
            actions.categoriesLoaded(safeCategories);
            categoryNames.clear();
            for (Category category : safeCategories) {
                categoryNames.put(category.getCategoryId(), category.getName());
            }
            UiCategoryOptions.fill(itemCategoryBox, safeCategories, true, "全部类型");
            UiCategoryOptions.fill(itemEditCategoryBox, safeCategories, false, "");
            UiCategoryOptions.fill(parentCategoryBox, safeCategories, true, "无上级分类");
            categoryModel.setRowCount(0);
            visibleCategories.clear();
            visibleCategories.addAll(safeCategories);
            Map<Long, String> paths = CategoryTreeFormatter.paths(safeCategories);
            for (Category category : safeCategories) {
                categoryModel.addRow(new Object[]{category.getCategoryId(), category.getName(),
                        category.getParentId() == null ? "-" : categoryName(category.getParentId()),
                        paths.get(category.getCategoryId())});
            }
            categoryTable.clearSelection();
            selectedCategory = null;
            updateCategoryButton.setEnabled(false);
            String message = completionMessage != null ? completionMessage
                    : "已加载 " + safeCategories.size() + " 个分类";
            loadItems(itemFilterSnapshot(), message);
        });
    }

    private void loadItems(ItemFilterSnapshot snapshot, String completionMessage) {
        taskExecutor.run("刷新景点", () -> actions.searchItems(snapshot.keyword(), snapshot.categoryId()), items -> {
            List<Item> safeItems = items == null ? List.of() : items;
            itemModel.setRowCount(0);
            visibleItems.clear();
            visibleItems.addAll(safeItems);
            for (Item item : safeItems) {
                itemModel.addRow(new Object[]{item.getItemId(), item.getTitle(), categoryName(item.getCategoryId()),
                        UiFormatters.money(item.getPrice()), UiFormatters.discount(item.getDiscountRate()),
                        UiFormatters.money(UiFormatters.discountedUnitPrice(item.getPrice(), item.getDiscountRate())),
                        UiFormatters.itemStatus(item.getStatus()), UiFormatters.date(item.getUpdatedAt())});
            }
            resetItemSelection();
            actions.setStatus(completionMessage != null ? completionMessage
                    : safeItems.isEmpty() ? "没有找到符合条件的景点" : "已加载 " + safeItems.size() + " 个景点");
        });
    }

    private void selectItem(Item item) {
        selectedItem = item;
        selectedItemLabel.setText("已选择：" + item.getTitle());
        itemTitleField.setText(item.getTitle());
        selectCategory(itemEditCategoryBox, item.getCategoryId());
        itemPriceField.setText(item.getPrice() == null ? "0.00" : item.getPrice().toPlainString());
        itemDiscountField.setText(item.getDiscountRate() == null
                ? "0" : item.getDiscountRate().stripTrailingZeros().toPlainString());
        itemStatusBox.setSelectedIndex(item.getStatus() != null && item.getStatus() == 1 ? 1 : 0);
        setItemActionsEnabled(true);
        long itemId = item.getItemId();
        taskExecutor.run("加载景点详情", () -> actions.itemDetail(itemId), detail -> {
            if (isSelectedItem(itemId)) {
                itemIntroArea.setText(UiFormatters.readableText(detail == null ? null : detail.get("description"), ""));
                itemImagesArea.setText(formatImages(detail));
                Document metadata = detail == null ? null : detail.get("metadata", Document.class);
                itemMetadataArea.setText(metadata == null || metadata.isEmpty() ? "{}" : metadata.toJson());
            }
        });
    }

    private void updateItemBasic() {
        long itemId = requireSelectedItemId();
        String title = itemTitleField.getText();
        Long categoryId = selectedCategoryId(itemEditCategoryBox);
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("请选择景点类型");
        }
        ItemFilterSnapshot filter = itemFilterSnapshot();
        taskExecutor.run("更新景点名称和分类", () -> actions.updateItem(itemId, title, categoryId), updated ->
                loadItems(filter, updated ? "景点名称和分类已更新" : "景点基本信息没有变化"));
    }

    private void updateItemPricing() {
        long itemId = requireSelectedItemId();
        String priceText = itemPriceField.getText();
        String discountText = itemDiscountField.getText();
        ItemFilterSnapshot filter = itemFilterSnapshot();
        taskExecutor.run("更新票价和优惠", () -> actions.updateItemPricing(itemId,
                UiInputParsers.requiredAmount(priceText, "票价"),
                UiInputParsers.requiredAmount(discountText, "优惠减免比例")), updated ->
                loadItems(filter, updated ? "景点票价和优惠已保存" : "票价和优惠没有变化"));
    }

    private void updateItemStatus() {
        long itemId = requireSelectedItemId();
        int status = itemStatusBox.getSelectedIndex();
        ItemFilterSnapshot filter = itemFilterSnapshot();
        taskExecutor.run("更新上下架状态", () -> actions.updateItemStatus(itemId, status), updated ->
                loadItems(filter, updated ? "景点上下架状态已更新" : "景点状态没有变化"));
    }

    private void updateItemDetail() {
        long itemId = requireSelectedItemId();
        String description = itemIntroArea.getText();
        String images = itemImagesArea.getText();
        String metadata = itemMetadataArea.getText();
        taskExecutor.run("更新景点详情", () -> actions.updateItemDetail(itemId, description, images, metadata),
                updated -> actions.setStatus(updated ? "景点简介、图片和扩展属性已更新" : "景点详情没有变化"));
    }

    private void createCategory() {
        String name = categoryNameField.getText();
        Long parentId = selectedCategoryId(parentCategoryBox);
        taskExecutor.run("新增分类", () -> actions.createCategory(name, parentId), id -> {
            categoryNameField.setText("");
            loadCategories("分类创建成功，编号：" + id);
        });
    }

    private void updateCategory() {
        if (selectedCategory == null || selectedCategory.getCategoryId() == null) {
            throw new IllegalArgumentException("请先从表格中选择分类");
        }
        long categoryId = selectedCategory.getCategoryId();
        String name = categoryNameField.getText();
        Long parentId = selectedCategoryId(parentCategoryBox);
        taskExecutor.run("更新分类", () -> {
            if (parentId != null && parentId == categoryId) {
                throw new IllegalArgumentException("分类不能选择自己作为上级分类");
            }
            return actions.updateCategory(categoryId, name, parentId);
        }, updated ->
                loadCategories(updated ? "分类名称和层级已更新" : "分类没有变化"));
    }

    private void resetItemSelection() {
        itemTable.clearSelection();
        selectedItem = null;
        selectedItemLabel.setText("请先从表格选择景点");
        setItemActionsEnabled(false);
        itemIntroArea.setText("");
        itemImagesArea.setText("");
        itemMetadataArea.setText("");
    }

    private void setItemActionsEnabled(boolean enabled) {
        basicItemButton.setEnabled(enabled);
        pricingButton.setEnabled(enabled);
        itemStatusButton.setEnabled(enabled);
        updateIntroButton.setEnabled(enabled);
    }

    private long requireSelectedItemId() {
        if (selectedItem == null || selectedItem.getItemId() == null) {
            throw new IllegalArgumentException("请先从表格中选择一个景点");
        }
        return selectedItem.getItemId();
    }

    private boolean isSelectedItem(long itemId) {
        return selectedItem != null && selectedItem.getItemId() != null && selectedItem.getItemId() == itemId;
    }

    private ItemFilterSnapshot itemFilterSnapshot() {
        return new ItemFilterSnapshot(itemKeywordField.getText(), selectedCategoryId(itemCategoryBox));
    }

    private String categoryName(Long categoryId) {
        return categoryId == null ? "-" : categoryNames.getOrDefault(categoryId, "未分类");
    }

    private static Long selectedCategoryId(JComboBox<CategoryOption> box) {
        Object selected = box.getSelectedItem();
        return selected instanceof CategoryOption option ? option.categoryId() : null;
    }

    private static void selectCategory(JComboBox<CategoryOption> box, Long categoryId) {
        for (int index = 0; index < box.getItemCount(); index++) {
            CategoryOption option = box.getItemAt(index);
            if (java.util.Objects.equals(option.categoryId(), categoryId)) {
                box.setSelectedIndex(index);
                return;
            }
        }
        if (box.getItemCount() > 0) {
            box.setSelectedIndex(0);
        }
    }

    private static String formatImages(Document detail) {
        if (detail == null) {
            return "";
        }
        List<?> images = detail.getList("images", Object.class);
        return images == null ? "" : images.stream().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
    }

    private static JTextArea textArea(int rows, int columns) {
        JTextArea area = new JTextArea(rows, columns);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private static DefaultTableModel itemModel() {
        return readOnlyModel("ID", "景点名称", "类型", "原价", "优惠", "折后价", "状态", "更新时间");
    }

    private static DefaultTableModel categoryModel() {
        return readOnlyModel("分类ID", "分类名称", "上级分类", "层级路径");
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    void setItemFilter(String keyword, int categoryIndex) {
        itemKeywordField.setText(keyword);
        itemCategoryBox.setSelectedIndex(categoryIndex);
    }

    void selectItemRow(int row) {
        itemTable.setRowSelectionInterval(row, row);
    }

    void setItemBasicForm(String title, int categoryIndex) {
        itemTitleField.setText(title);
        itemEditCategoryBox.setSelectedIndex(categoryIndex);
    }

    void setItemPricingForm(String price, String discount) {
        itemPriceField.setText(price);
        itemDiscountField.setText(discount);
    }

    void selectCategoryRow(int row) {
        categoryTable.setRowSelectionInterval(row, row);
    }

    void setCategoryForm(String name, int parentIndex) {
        categoryNameField.setText(name);
        parentCategoryBox.setSelectedIndex(parentIndex);
    }

    int itemRowCount() {
        return itemModel.getRowCount();
    }

    int categoryRowCount() {
        return categoryModel.getRowCount();
    }

    boolean itemActionsEnabled() {
        return basicItemButton.isEnabled() && pricingButton.isEnabled()
                && itemStatusButton.isEnabled() && updateIntroButton.isEnabled();
    }

    boolean categoryUpdateEnabled() {
        return updateCategoryButton.isEnabled();
    }

    String itemIntroText() {
        return itemIntroArea.getText();
    }

    public interface Actions {
        List<Category> listCategories();

        void categoriesLoaded(List<Category> categories);

        List<Item> searchItems(String keyword, Long categoryId);

        Document itemDetail(long itemId);

        void showCreateItemDialog(Runnable refreshItems);

        boolean updateItem(long itemId, String title, long categoryId);

        boolean updateItemPricing(long itemId, BigDecimal price, BigDecimal discount);

        boolean updateItemStatus(long itemId, int status);

        boolean updateItemDetail(long itemId, String description, String images, String metadata);

        long createCategory(String name, Long parentId);

        boolean updateCategory(long categoryId, String name, Long parentId);

        void setStatus(String message);
    }

    private record ItemFilterSnapshot(String keyword, Long categoryId) {
    }
}
