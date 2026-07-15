package com.scenicticket.ui;

import com.scenicticket.dto.OrderViewDTO;
import com.scenicticket.model.Order;
import org.junit.jupiter.api.Test;

import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OrderTableModelsTest {
    @Test
    void userOrderTableKeepsTicketTypeAndVisitDateColumns() {
        DefaultTableModel model = OrderTableModels.create(false);
        OrderTableModels.fill(model, List.of(view()), false);

        assertEquals("票种", model.getColumnName(2));
        assertEquals("游玩日期", model.getColumnName(3));
        assertEquals("学生票", model.getValueAt(0, 2));
        assertEquals("2026-08-01", model.getValueAt(0, 3));
        assertEquals(11, model.getColumnCount());
        assertFalse(model.isCellEditable(0, 0));
    }

    @Test
    void adminOrderTableIncludesUserIdBeforeItemTitle() {
        DefaultTableModel model = OrderTableModels.create(true);
        OrderTableModels.fill(model, List.of(view()), true);

        assertEquals("用户ID", model.getColumnName(1));
        assertEquals(2L, model.getValueAt(0, 1));
        assertEquals("太湖", model.getValueAt(0, 2));
        assertEquals("学生票", model.getValueAt(0, 3));
        assertEquals("2026-08-01", model.getValueAt(0, 4));
        assertEquals(12, model.getColumnCount());
    }

    private static OrderViewDTO view() {
        Order order = new Order();
        order.setOrderId(11L);
        order.setUserId(2L);
        order.setTicketTypeNameSnapshot("学生票");
        order.setVisitDate(LocalDate.of(2026, 8, 1));
        order.setQuantity(2);
        order.setOriginalUnitPrice(new BigDecimal("100.00"));
        order.setDiscountRate(new BigDecimal("20"));
        order.setDiscountedUnitPrice(new BigDecimal("80.00"));
        order.setAmount(new BigDecimal("160.00"));
        order.setPaymentMethod("微信");
        order.setStatus(0);
        order.setExpiresAt(LocalDateTime.of(2026, 7, 31, 10, 15));
        order.setCreatedAt(LocalDateTime.of(2026, 7, 31, 10, 0));

        OrderViewDTO view = new OrderViewDTO();
        view.setOrder(order);
        view.setItemTitle("太湖");
        return view;
    }
}
