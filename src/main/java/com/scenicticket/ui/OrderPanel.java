package com.scenicticket.ui;

import com.scenicticket.dto.OrderActionResult;
import com.scenicticket.dto.OrderViewDTO;
import com.scenicticket.model.Order;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class OrderPanel extends JPanel {
    private final long actorUserId;
    private final boolean adminView;
    private final UiTaskExecutor taskExecutor;
    private final Actions actions;
    private final Supplier<LocalDate> todaySupplier;
    private final JTextField userIdField = new JTextField(10);
    private final JTextField orderIdField = new JTextField(10);
    private final JComboBox<String> statusBox = new JComboBox<>(
            new String[]{"全部状态", "0-待支付", "1-已支付", "2-已取消", "3-已完成"});
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel selectedOrderLabel = new JLabel("请先从表格选择订单");
    private final JButton payButton = UiComponents.primaryButton("确认支付");
    private final JButton cancelButton = UiComponents.secondaryButton("取消待支付订单");
    private final JButton refundButton = UiComponents.secondaryButton("申请模拟退款");
    private final List<OrderViewDTO> visibleOrders = new ArrayList<>();
    private Long selectedOrderId;

    public OrderPanel(long actorUserId, boolean adminView, UiTaskExecutor taskExecutor, Actions actions) {
        this(actorUserId, adminView, taskExecutor, actions, LocalDate::now);
    }

    OrderPanel(long actorUserId, boolean adminView, UiTaskExecutor taskExecutor,
               Actions actions, Supplier<LocalDate> todaySupplier) {
        super(new BorderLayout(12, 12));
        if (actorUserId <= 0) {
            throw new IllegalArgumentException("当前用户无效，请重新登录");
        }
        this.actorUserId = actorUserId;
        this.adminView = adminView;
        this.taskExecutor = taskExecutor;
        this.actions = actions;
        this.todaySupplier = todaySupplier;
        this.tableModel = OrderTableModels.create(adminView);
        this.table = UiComponents.table(tableModel);
        setBackground(UiTheme.BACKGROUND);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        configureTable();
        configureActions();
        add(createControls(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private JPanel createControls() {
        JPanel queryToolbar = UiComponents.toolbar();
        JButton queryButton = UiComponents.primaryButton("查询订单");
        JButton refreshButton = UiComponents.secondaryButton("刷新");
        JButton resetButton = UiComponents.secondaryButton("重置");
        if (adminView) {
            queryToolbar.add(new JLabel("用户ID"));
            queryToolbar.add(userIdField);
        }
        queryToolbar.add(new JLabel("订单号"));
        queryToolbar.add(orderIdField);
        queryToolbar.add(new JLabel("状态"));
        queryToolbar.add(statusBox);
        queryToolbar.add(queryButton);
        queryToolbar.add(refreshButton);
        queryToolbar.add(resetButton);

        queryButton.addActionListener(event -> refreshOrders());
        refreshButton.addActionListener(event -> refreshOrders());
        resetButton.addActionListener(event -> {
            userIdField.setText("");
            orderIdField.setText("");
            statusBox.setSelectedIndex(0);
            refreshOrders();
        });
        orderIdField.addActionListener(event -> refreshOrders());

        JPanel actionToolbar = UiComponents.toolbar();
        actionToolbar.add(selectedOrderLabel);
        actionToolbar.add(payButton);
        actionToolbar.add(cancelButton);
        actionToolbar.add(refundButton);
        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 8));
        controls.setOpaque(false);
        controls.add(UiComponents.card("订单查询", queryToolbar));
        controls.add(UiComponents.card("订单生命周期操作", actionToolbar));
        return controls;
    }

    private void configureTable() {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int[] widths = OrderTableModels.columnWidths(adminView);
        for (int index = 0; index < widths.length; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || table.getSelectedRow() < 0) {
                return;
            }
            int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
            if (modelRow < 0 || modelRow >= visibleOrders.size()) {
                return;
            }
            selectOrder(visibleOrders.get(modelRow).getOrder());
        });
    }

    private void configureActions() {
        setActionAvailability(OrderActionPolicy.Availability.NONE);
        payButton.addActionListener(event -> {
            long orderId = requireSelectedOrderId();
            runLifecycleAction("确认支付", () -> actions.pay(orderId));
        });
        cancelButton.addActionListener(event -> {
            long orderId = requireSelectedOrderId();
            runLifecycleAction("取消待支付订单", () -> actions.cancelPending(orderId));
        });
        refundButton.addActionListener(event -> {
            long orderId = requireSelectedOrderId();
            String reason = actions.requestRefundReason();
            if (reason != null) {
                runLifecycleAction("模拟退款", () -> actions.refund(orderId, reason));
            }
        });
    }

    private void refreshOrders() {
        String userIdText = userIdField.getText();
        String orderIdText = orderIdField.getText();
        int statusIndex = statusBox.getSelectedIndex();
        taskExecutor.run("订单查询", () -> {
            Long queryUserId;
            if (adminView) {
                queryUserId = UiInputParsers.optionalLong(userIdText);
            } else {
                queryUserId = actorUserId;
            }
            Long queryOrderId = UiInputParsers.optionalLong(orderIdText);
            Integer status = statusIndex <= 0 ? null : statusIndex - 1;
            return actions.search(queryUserId, queryOrderId, status);
        }, this::showOrders);
    }

    private void showOrders(List<OrderViewDTO> orderViews) {
        List<OrderViewDTO> safeViews = orderViews == null ? List.of() : orderViews;
        visibleOrders.clear();
        visibleOrders.addAll(safeViews);
        OrderTableModels.fill(tableModel, safeViews, adminView);
        resetSelection();
        actions.setStatus("查询到 " + safeViews.size() + " 条订单");
    }

    private void selectOrder(Order order) {
        if (order == null || order.getOrderId() == null) {
            resetSelection();
            return;
        }
        selectedOrderId = order.getOrderId();
        selectedOrderLabel.setText("已选择订单：" + selectedOrderId);
        setActionAvailability(OrderActionPolicy.evaluate(actorUserId, order, todaySupplier.get()));
    }

    private void runLifecycleAction(String taskName, Supplier<OrderActionResult> operation) {
        taskExecutor.run(taskName, operation::get, result -> {
            actions.setStatus(result.message());
            refreshOrders();
        });
    }

    private void resetSelection() {
        table.clearSelection();
        selectedOrderId = null;
        selectedOrderLabel.setText("请先从表格选择订单");
        setActionAvailability(OrderActionPolicy.Availability.NONE);
    }

    private void setActionAvailability(OrderActionPolicy.Availability availability) {
        payButton.setEnabled(availability.canPay());
        cancelButton.setEnabled(availability.canCancel());
        refundButton.setEnabled(availability.canRefund());
    }

    private long requireSelectedOrderId() {
        if (selectedOrderId == null) {
            throw new IllegalArgumentException("请先从表格中选择一个订单");
        }
        return selectedOrderId;
    }

    int rowCount() {
        return tableModel.getRowCount();
    }

    void selectRow(int row) {
        table.setRowSelectionInterval(row, row);
    }

    boolean payEnabled() {
        return payButton.isEnabled();
    }

    boolean cancelEnabled() {
        return cancelButton.isEnabled();
    }

    boolean refundEnabled() {
        return refundButton.isEnabled();
    }

    public interface Actions {
        List<OrderViewDTO> search(Long queryUserId, Long orderId, Integer status);

        OrderActionResult pay(long orderId);

        OrderActionResult cancelPending(long orderId);

        OrderActionResult refund(long orderId, String reason);

        String requestRefundReason();

        void setStatus(String message);
    }
}
