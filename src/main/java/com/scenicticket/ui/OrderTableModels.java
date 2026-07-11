package com.scenicticket.ui;

import com.scenicticket.dto.OrderViewDTO;
import com.scenicticket.model.Order;

import javax.swing.table.DefaultTableModel;
import java.util.List;

final class OrderTableModels {
    private static final String[] ADMIN_COLUMNS = {
            "\u8ba2\u5355\u53f7", "\u7528\u6237ID", "\u666f\u70b9\u540d\u79f0",
            "\u7968\u79cd", "\u6e38\u73a9\u65e5\u671f", "\u7968\u6570",
            "\u539f\u4ef7", "\u4f18\u60e0", "\u6298\u540e\u5355\u4ef7",
            "\u603b\u989d", "\u4ed8\u6b3e\u65b9\u5f0f", "\u72b6\u6001",
            "\u8fc7\u671f\u65f6\u95f4", "\u521b\u5efa\u65f6\u95f4"
    };

    private static final String[] USER_COLUMNS = {
            "\u8ba2\u5355\u53f7", "\u666f\u70b9\u540d\u79f0",
            "\u7968\u79cd", "\u6e38\u73a9\u65e5\u671f", "\u7968\u6570",
            "\u539f\u4ef7", "\u4f18\u60e0", "\u6298\u540e\u5355\u4ef7",
            "\u603b\u989d", "\u4ed8\u6b3e\u65b9\u5f0f", "\u72b6\u6001",
            "\u8fc7\u671f\u65f6\u95f4", "\u521b\u5efa\u65f6\u95f4"
    };

    private OrderTableModels() {
    }

    static DefaultTableModel create(boolean admin) {
        return new DefaultTableModel(admin ? ADMIN_COLUMNS : USER_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static int[] columnWidths(boolean admin) {
        return admin
                ? new int[]{80, 80, 170, 100, 105, 55, 85, 75, 90, 95, 85, 85, 155, 155}
                : new int[]{80, 180, 100, 105, 55, 85, 75, 90, 95, 85, 85, 155, 155};
    }

    static void fill(DefaultTableModel model, List<OrderViewDTO> orderViews, boolean admin) {
        model.setRowCount(0);
        for (OrderViewDTO view : orderViews) {
            Order order = view.getOrder();
            if (admin) {
                model.addRow(new Object[]{order.getOrderId(), order.getUserId(), view.getItemTitle(),
                        valueText(order.getTicketTypeNameSnapshot()), valueText(order.getVisitDate()), order.getQuantity(),
                        UiFormatters.money(order.getOriginalUnitPrice()), UiFormatters.discount(order.getDiscountRate()),
                        UiFormatters.money(order.getDiscountedUnitPrice()), UiFormatters.money(order.getAmount()),
                        order.getPaymentMethod(), UiFormatters.orderStatus(order.getStatus()),
                        UiFormatters.date(order.getExpiresAt()), UiFormatters.date(order.getCreatedAt())});
            } else {
                model.addRow(new Object[]{order.getOrderId(), view.getItemTitle(),
                        valueText(order.getTicketTypeNameSnapshot()), valueText(order.getVisitDate()), order.getQuantity(),
                        UiFormatters.money(order.getOriginalUnitPrice()), UiFormatters.discount(order.getDiscountRate()),
                        UiFormatters.money(order.getDiscountedUnitPrice()), UiFormatters.money(order.getAmount()),
                        order.getPaymentMethod(), UiFormatters.orderStatus(order.getStatus()),
                        UiFormatters.date(order.getExpiresAt()), UiFormatters.date(order.getCreatedAt())});
            }
        }
    }

    private static String valueText(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
