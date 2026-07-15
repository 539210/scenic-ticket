package com.scenicticket.ui;

import com.scenicticket.dto.MonthlyOrderDetailDTO;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.List;

public final class MonthlyOrderDetailDialogPanel extends JPanel {
    private static final String[] COLUMNS = {
            "订单号", "购买用户", "用户ID", "景点", "票种", "游玩日期",
            "票数", "单价", "总额", "付款方式", "状态", "下单时间"
    };

    public MonthlyOrderDetailDialogPanel(LocalDate orderDate, List<MonthlyOrderDetailDTO> details) {
        super(new BorderLayout(0, 10));
        List<MonthlyOrderDetailDTO> safeDetails = details == null ? List.of() : details;
        JLabel summary = new JLabel(orderDate + "，共 " + safeDetails.size() + " 笔有效订单");
        summary.setFont(UiTheme.SECTION_FONT);
        add(summary, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (MonthlyOrderDetailDTO detail : safeDetails) {
            model.addRow(new Object[]{
                    detail.orderId(), detail.username(), detail.userId(), detail.itemTitle(),
                    detail.ticketTypeName(), detail.visitDate() == null ? "-" : detail.visitDate(),
                    detail.quantity(), UiFormatters.money(detail.unitPrice()), UiFormatters.money(detail.amount()),
                    detail.paymentMethod(), UiFormatters.orderStatus(detail.status()), UiFormatters.date(detail.createdAt())
            });
        }
        JTable table = UiComponents.table(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {75, 110, 70, 170, 85, 95, 50, 80, 90, 85, 75, 145};
        for (int index = 0; index < widths.length; index++) {
            table.getColumnModel().getColumn(index).setPreferredWidth(widths[index]);
        }
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(1_180, Math.min(420, 95 + safeDetails.size() * 28)));
        add(scrollPane, BorderLayout.CENTER);
    }
}
