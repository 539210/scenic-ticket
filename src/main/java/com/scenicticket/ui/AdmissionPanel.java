package com.scenicticket.ui;

import com.scenicticket.dto.AdmissionResult;
import com.scenicticket.model.Admission;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;

public final class AdmissionPanel extends JPanel {
    private final UiTaskExecutor taskExecutor;
    private final Actions actions;
    private final JTextField orderIdField = new JTextField(10);
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JTextField noteField = new JTextField(24);
    private final DefaultTableModel tableModel = tableModel();

    public AdmissionPanel(UiTaskExecutor taskExecutor, Actions actions) {
        super(new BorderLayout(10, 10));
        this.taskExecutor = taskExecutor;
        this.actions = actions;
        setOpaque(false);
        add(UiComponents.card("管理员门票核销（仅游玩日期当天的已支付订单）", createToolbar()),
                BorderLayout.NORTH);
        add(UiComponents.card("核销记录", UiComponents.scroll(UiComponents.table(tableModel))), BorderLayout.CENTER);
    }

    private JPanel createToolbar() {
        JPanel toolbar = UiComponents.toolbar();
        JButton queryButton = UiComponents.secondaryButton("查询核销记录");
        JButton admitButton = UiComponents.primaryButton("确认核销");
        toolbar.add(new JLabel("订单ID"));
        toolbar.add(orderIdField);
        toolbar.add(new JLabel("本次数量"));
        toolbar.add(quantitySpinner);
        toolbar.add(new JLabel("备注"));
        toolbar.add(noteField);
        toolbar.add(queryButton);
        toolbar.add(admitButton);
        queryButton.addActionListener(event -> loadAdmissions(orderIdField.getText(), null));
        orderIdField.addActionListener(event -> loadAdmissions(orderIdField.getText(), null));
        admitButton.addActionListener(event -> admit());
        return toolbar;
    }

    private void loadAdmissions(String orderIdText, String completionMessage) {
        taskExecutor.run("查询核销记录", () -> {
            long orderId = UiInputParsers.requiredLong(orderIdText, "订单ID");
            return new AdmissionQueryResult(orderId, actions.listByOrder(orderId));
        }, result -> showAdmissions(result.orderId(), result.admissions(), completionMessage));
    }

    private void showAdmissions(long orderId, List<Admission> admissions, String completionMessage) {
        List<Admission> safeAdmissions = admissions == null ? List.of() : admissions;
        tableModel.setRowCount(0);
        for (Admission admission : safeAdmissions) {
            tableModel.addRow(new Object[]{admission.getAdmissionId(), admission.getOrderId(), admission.getQuantity(),
                    admission.getOperatorUserId(), UiFormatters.date(admission.getAdmittedAt()),
                    valueText(admission.getNote())});
        }
        if (completionMessage != null && !completionMessage.isBlank()) {
            actions.setStatus(completionMessage);
        } else {
            actions.setStatus(safeAdmissions.isEmpty()
                    ? "订单 " + orderId + " 暂无核销记录"
                    : "已加载 " + safeAdmissions.size() + " 条核销记录");
        }
    }

    private void admit() {
        String orderIdText = orderIdField.getText();
        int quantity = (Integer) quantitySpinner.getValue();
        String note = noteField.getText();
        taskExecutor.run("门票核销", () -> {
            long orderId = UiInputParsers.requiredLong(orderIdText, "订单ID");
            return actions.admit(orderId, quantity, note);
        }, result -> {
            loadAdmissions(String.valueOf(result.orderId()), result.message());
        });
    }

    private static String valueText(Object value) {
        return value == null || String.valueOf(value).isBlank() ? "-" : String.valueOf(value);
    }

    private static DefaultTableModel tableModel() {
        return new DefaultTableModel(
                new Object[]{"核销ID", "订单ID", "数量", "操作员ID", "核销时间", "备注"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    void setForm(String orderId, int quantity, String note) {
        orderIdField.setText(orderId);
        quantitySpinner.setValue(quantity);
        noteField.setText(note);
    }

    int rowCount() {
        return tableModel.getRowCount();
    }

    Object tableValueAt(int row, int column) {
        return tableModel.getValueAt(row, column);
    }

    public interface Actions {
        List<Admission> listByOrder(long orderId);

        AdmissionResult admit(long orderId, int quantity, String note);

        void setStatus(String message);
    }

    private record AdmissionQueryResult(long orderId, List<Admission> admissions) {
    }
}
