package com.scenicticket.ui;

import com.scenicticket.model.TicketInventory;
import com.scenicticket.model.TicketType;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class TicketInventoryPanel extends JPanel {
    private final UiTaskExecutor taskExecutor;
    private final Actions actions;
    private final JTextField itemIdField = new JTextField("1", 7);
    private final JTextField typeNameField = new JTextField(12);
    private final JTextField typePriceField = new JTextField("80.00", 8);
    private final JTextField typeDiscountField = new JTextField("0", 6);
    private final JComboBox<String> typeStatusBox = new JComboBox<>(new String[]{"下架", "上架"});
    private final JButton updateTypeButton = UiComponents.secondaryButton("更新所选票种");
    private final DefaultTableModel typeModel = typeModel();
    private final JTable typeTable = UiComponents.table(typeModel);
    private final List<TicketType> visibleTypes = new ArrayList<>();
    private TicketType selectedType;

    private final JTextField inventoryTypeIdField = new JTextField(8);
    private final JTextField startDateField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField endDateField = new JTextField(LocalDate.now().plusDays(14).toString(), 10);
    private final JTextField maintainDateField = new JTextField(LocalDate.now().plusDays(1).toString(), 10);
    private final JTextField totalStockField = new JTextField("100", 8);
    private final DefaultTableModel inventoryModel = inventoryModel();

    public TicketInventoryPanel(UiTaskExecutor taskExecutor, Actions actions) {
        super(new BorderLayout(12, 12));
        this.taskExecutor = taskExecutor;
        this.actions = actions;
        setOpaque(false);
        typeStatusBox.setSelectedIndex(1);
        updateTypeButton.setEnabled(false);
        configureTypeSelection();
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("票种管理", createTypePage());
        tabs.addTab("每日库存", createInventoryPage());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createTypePage() {
        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setOpaque(false);
        JPanel toolbar = UiComponents.toolbar();
        JButton queryButton = UiComponents.primaryButton("查询票种");
        JButton createButton = UiComponents.primaryButton("新增票种");
        toolbar.add(new JLabel("景点ID"));
        toolbar.add(itemIdField);
        toolbar.add(new JLabel("票种名称"));
        toolbar.add(typeNameField);
        toolbar.add(new JLabel("原价"));
        toolbar.add(typePriceField);
        toolbar.add(new JLabel("优惠减免%"));
        toolbar.add(typeDiscountField);
        toolbar.add(new JLabel("状态"));
        toolbar.add(typeStatusBox);
        toolbar.add(queryButton);
        toolbar.add(createButton);
        toolbar.add(updateTypeButton);
        queryButton.addActionListener(event -> loadTypes(itemIdField.getText(), null));
        itemIdField.addActionListener(event -> loadTypes(itemIdField.getText(), null));
        createButton.addActionListener(event -> createType());
        updateTypeButton.addActionListener(event -> updateType());
        page.add(UiComponents.card("票种维护", toolbar), BorderLayout.NORTH);
        page.add(UiComponents.card("票种列表", UiComponents.scroll(typeTable)), BorderLayout.CENTER);
        return page;
    }

    private JPanel createInventoryPage() {
        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setOpaque(false);
        JPanel toolbar = UiComponents.toolbar();
        JButton queryButton = UiComponents.primaryButton("查询库存");
        JButton saveButton = UiComponents.primaryButton("设置总库存");
        toolbar.add(new JLabel("票种ID"));
        toolbar.add(inventoryTypeIdField);
        toolbar.add(new JLabel("开始日期"));
        toolbar.add(startDateField);
        toolbar.add(new JLabel("结束日期"));
        toolbar.add(endDateField);
        toolbar.add(queryButton);
        toolbar.add(new JLabel("维护日期"));
        toolbar.add(maintainDateField);
        toolbar.add(new JLabel("总库存"));
        toolbar.add(totalStockField);
        toolbar.add(saveButton);
        queryButton.addActionListener(event -> loadInventory(inventorySnapshot(), null));
        inventoryTypeIdField.addActionListener(event -> loadInventory(inventorySnapshot(), null));
        saveButton.addActionListener(event -> saveInventory());
        page.add(UiComponents.card("按日期维护库存（日期格式 yyyy-MM-dd）", toolbar), BorderLayout.NORTH);
        page.add(UiComponents.card("每日库存列表", UiComponents.scroll(UiComponents.table(inventoryModel))),
                BorderLayout.CENTER);
        return page;
    }

    private void configureTypeSelection() {
        typeTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || typeTable.getSelectedRow() < 0) {
                return;
            }
            int modelRow = typeTable.convertRowIndexToModel(typeTable.getSelectedRow());
            if (modelRow < 0 || modelRow >= visibleTypes.size()) {
                return;
            }
            selectedType = visibleTypes.get(modelRow);
            itemIdField.setText(String.valueOf(selectedType.getItemId()));
            typeNameField.setText(selectedType.getName());
            typePriceField.setText(selectedType.getOriginalPrice().toPlainString());
            typeDiscountField.setText(selectedType.getDiscountRate().stripTrailingZeros().toPlainString());
            typeStatusBox.setSelectedIndex(selectedType.getStatus() != null && selectedType.getStatus() == 1 ? 1 : 0);
            inventoryTypeIdField.setText(String.valueOf(selectedType.getTicketTypeId()));
            updateTypeButton.setEnabled(true);
        });
    }

    private void loadTypes(String itemIdText, String completionMessage) {
        taskExecutor.run("查询票种", () -> {
            long itemId = UiInputParsers.requiredLong(itemIdText, "景点ID");
            return new TicketTypeQueryResult(actions.listTypes(itemId));
        }, result -> showTypes(result.types(), completionMessage));
    }

    private void showTypes(List<TicketType> types, String completionMessage) {
        List<TicketType> safeTypes = types == null ? List.of() : types;
        typeModel.setRowCount(0);
        visibleTypes.clear();
        visibleTypes.addAll(safeTypes);
        for (TicketType type : safeTypes) {
            typeModel.addRow(new Object[]{type.getTicketTypeId(), type.getItemId(), type.getName(),
                    UiFormatters.money(type.getOriginalPrice()), UiFormatters.discount(type.getDiscountRate()),
                    UiFormatters.money(UiFormatters.discountedUnitPrice(type.getOriginalPrice(), type.getDiscountRate())),
                    UiFormatters.itemStatus(type.getStatus())});
        }
        typeTable.clearSelection();
        selectedType = null;
        updateTypeButton.setEnabled(false);
        actions.setStatus(completionMessage != null ? completionMessage
                : safeTypes.isEmpty() ? "该景点暂无票种" : "已加载 " + safeTypes.size() + " 个票种");
    }

    private void createType() {
        TicketTypeFormSnapshot snapshot = typeFormSnapshot();
        taskExecutor.run("新增票种", () -> {
            long itemId = snapshot.itemId();
            long id = actions.createType(itemId, snapshot.name(), snapshot.price(), snapshot.discount());
            return new TicketTypeMutationResult(itemId, "票种创建成功，编号：" + id);
        }, result -> loadTypes(String.valueOf(result.itemId()), result.message()));
    }

    private void updateType() {
        if (selectedType == null || selectedType.getTicketTypeId() == null) {
            throw new IllegalArgumentException("请先选择票种");
        }
        long ticketTypeId = selectedType.getTicketTypeId();
        TicketTypeFormSnapshot snapshot = typeFormSnapshot();
        taskExecutor.run("更新票种", () -> {
            boolean updated = actions.updateType(ticketTypeId, snapshot.name(), snapshot.price(),
                    snapshot.discount(), snapshot.status());
            return new TicketTypeMutationResult(snapshot.itemId(), updated ? "票种已更新" : "票种没有变化");
        }, result -> loadTypes(String.valueOf(result.itemId()), result.message()));
    }

    private TicketTypeFormSnapshot typeFormSnapshot() {
        return new TicketTypeFormSnapshot(itemIdField.getText(), typeNameField.getText(),
                typePriceField.getText(), typeDiscountField.getText(), typeStatusBox.getSelectedIndex());
    }

    private InventoryRangeSnapshot inventorySnapshot() {
        return new InventoryRangeSnapshot(inventoryTypeIdField.getText(), startDateField.getText(), endDateField.getText());
    }

    private void loadInventory(InventoryRangeSnapshot snapshot, String completionMessage) {
        taskExecutor.run("查询每日库存", () -> actions.listInventory(
                snapshot.ticketTypeId(), snapshot.startDate(), snapshot.endDate()), inventories -> {
            List<TicketInventory> safeInventories = inventories == null ? List.of() : inventories;
            inventoryModel.setRowCount(0);
            for (TicketInventory inventory : safeInventories) {
                inventoryModel.addRow(new Object[]{inventory.getInventoryId(), inventory.getTicketTypeId(),
                        inventory.getVisitDate(), inventory.getTotalStock(), inventory.getAvailableStock(),
                        inventory.getReservedStock(), inventory.getSoldStock(), inventory.getVersion()});
            }
            actions.setStatus(completionMessage != null ? completionMessage
                    : safeInventories.isEmpty() ? "所选日期范围暂无库存记录"
                    : "已加载 " + safeInventories.size() + " 条每日库存");
        });
    }

    private void saveInventory() {
        InventoryRangeSnapshot rangeSnapshot = inventorySnapshot();
        String maintainDateText = maintainDateField.getText();
        String totalStockText = totalStockField.getText();
        taskExecutor.run("设置每日库存", () -> actions.setTotalStock(rangeSnapshot.ticketTypeId(),
                UiInputParsers.requiredDate(maintainDateText, "维护日期"),
                UiInputParsers.requiredInt(totalStockText, "总库存")), inventory ->
                loadInventory(rangeSnapshot, "库存已保存，可售 " + inventory.getAvailableStock() + " 张"));
    }

    private static DefaultTableModel typeModel() {
        return readOnlyModel("票种ID", "景点ID", "票种名称", "原价", "优惠", "折后价", "状态");
    }

    private static DefaultTableModel inventoryModel() {
        return readOnlyModel("库存ID", "票种ID", "游玩日期", "总库存", "可售", "已预留", "已售", "版本");
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    void setTypeForm(String itemId, String name, String price, String discount, int statusIndex) {
        itemIdField.setText(itemId);
        typeNameField.setText(name);
        typePriceField.setText(price);
        typeDiscountField.setText(discount);
        typeStatusBox.setSelectedIndex(statusIndex);
    }

    void setInventoryForm(String ticketTypeId, String startDate, String endDate,
                          String maintainDate, String totalStock) {
        inventoryTypeIdField.setText(ticketTypeId);
        startDateField.setText(startDate);
        endDateField.setText(endDate);
        maintainDateField.setText(maintainDate);
        totalStockField.setText(totalStock);
    }

    void selectTypeRow(int row) {
        typeTable.setRowSelectionInterval(row, row);
    }

    boolean updateEnabled() {
        return updateTypeButton.isEnabled();
    }

    String selectedTypeName() {
        return typeNameField.getText();
    }

    String inventoryTypeId() {
        return inventoryTypeIdField.getText();
    }

    int typeRowCount() {
        return typeModel.getRowCount();
    }

    int inventoryRowCount() {
        return inventoryModel.getRowCount();
    }

    Object inventoryValueAt(int row, int column) {
        return inventoryModel.getValueAt(row, column);
    }

    public interface Actions {
        List<TicketType> listTypes(long itemId);

        long createType(long itemId, String name, BigDecimal price, BigDecimal discount);

        boolean updateType(long ticketTypeId, String name, BigDecimal price, BigDecimal discount, int status);

        List<TicketInventory> listInventory(long ticketTypeId, LocalDate startDate, LocalDate endDate);

        TicketInventory setTotalStock(long ticketTypeId, LocalDate visitDate, int totalStock);

        void setStatus(String message);
    }

    private record TicketTypeQueryResult(List<TicketType> types) {
    }

    private record TicketTypeMutationResult(long itemId, String message) {
    }

    private record TicketTypeFormSnapshot(String itemIdText, String name,
                                          String priceText, String discountText, int status) {
        private long itemId() {
            return UiInputParsers.requiredLong(itemIdText, "景点ID");
        }

        private BigDecimal price() {
            return UiInputParsers.requiredAmount(priceText, "票价");
        }

        private BigDecimal discount() {
            return UiInputParsers.requiredAmount(discountText, "优惠减免比例");
        }
    }

    private record InventoryRangeSnapshot(String ticketTypeIdText, String startDateText, String endDateText) {
        private long ticketTypeId() {
            return UiInputParsers.requiredLong(ticketTypeIdText, "票种ID");
        }

        private LocalDate startDate() {
            return UiInputParsers.requiredDate(startDateText, "开始日期");
        }

        private LocalDate endDate() {
            return UiInputParsers.requiredDate(endDateText, "结束日期");
        }
    }
}
