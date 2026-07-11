package com.scenicticket.integration;

import com.scenicticket.config.DBConfig;
import com.scenicticket.config.DatabaseTargetGuard;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dao.mysql.RefundDAO;
import com.scenicticket.dao.mysql.TicketInventoryDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.Order;
import com.scenicticket.model.TicketInventory;
import com.scenicticket.service.OrderLifecycleService;
import com.scenicticket.service.TicketInventoryService;
import com.scenicticket.util.MySQLDBUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderLifecycleIntegrationTest {
    @BeforeAll
    static void verifyTargets() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("integrationTests", "false")),
                "Set -DintegrationTests=true with explicit scenic_ticket_test overrides.");
        DatabaseTargetGuard.requireIsolatedTestTargets(DBConfig.get("mysql.url"), DBConfig.get("mongodb.database"));
    }

    @Test
    void pendingPaymentRefundCancellationAndExpiryKeepInventoryConsistent() throws Exception {
        TicketInventoryService inventoryService = new TicketInventoryService();
        OrderLifecycleService lifecycle = new OrderLifecycleService();
        String name = "流程票-" + UUID.randomUUID().toString().substring(0, 8);
        LocalDate visitDate = LocalDate.now().plusDays(400);
        long ticketTypeId = inventoryService.createTicketType(1L, 1L, name,
                new BigDecimal("100.00"), new BigDecimal("20.00"));
        List<Long> orderIds = new ArrayList<>();
        try {
            inventoryService.setTotalStock(1L, ticketTypeId, visitDate, 20);

            long paidOrderId = lifecycle.createPendingOrder(2L, ticketTypeId, visitDate, 3,
                    "微信", "127.0.0.1").orderId();
            orderIds.add(paidOrderId);
            assertOrderAndStock(paidOrderId, 0, ticketTypeId, visitDate, 17, 3, 0);

            lifecycle.pay(2L, paidOrderId, "127.0.0.1");
            Order paid = assertOrderAndStock(paidOrderId, 1, ticketTypeId, visitDate, 17, 0, 3);
            assertNotNull(paid.getPaidAt());
            assertEquals(name, paid.getTicketTypeNameSnapshot());
            assertEquals(new BigDecimal("100.00"), paid.getOriginalUnitPrice());
            assertEquals(new BigDecimal("80.00"), paid.getDiscountedUnitPrice());
            assertEquals(new BigDecimal("240.00"), paid.getAmount());

            lifecycle.refund(2L, paidOrderId, "集成测试退款", "127.0.0.1");
            Order refunded = assertOrderAndStock(paidOrderId, 2, ticketTypeId, visitDate, 20, 0, 0);
            assertNotNull(refunded.getRefundedAt());
            try (Connection connection = MySQLDBUtil.getConnection()) {
                assertTrue(new RefundDAO().findByOrderId(connection, paidOrderId).isPresent());
            }
            assertThrows(BusinessException.class,
                    () -> lifecycle.pay(2L, paidOrderId, "127.0.0.1"));

            long cancelledOrderId = lifecycle.createPendingOrder(2L, ticketTypeId, visitDate, 2,
                    "支付宝", "127.0.0.1").orderId();
            orderIds.add(cancelledOrderId);
            lifecycle.cancelPending(2L, cancelledOrderId, "127.0.0.1");
            assertOrderAndStock(cancelledOrderId, 2, ticketTypeId, visitDate, 20, 0, 0);

            long expiredOrderId = lifecycle.createPendingOrder(2L, ticketTypeId, visitDate, 1,
                    "银行卡", "127.0.0.1").orderId();
            orderIds.add(expiredOrderId);
            forceExpired(expiredOrderId);
            assertTrue(lifecycle.expireDueOrders(20) >= 1);
            assertOrderAndStock(expiredOrderId, 2, ticketTypeId, visitDate, 20, 0, 0);
        } finally {
            cleanup(orderIds, ticketTypeId, visitDate);
        }
    }

    private static Order assertOrderAndStock(long orderId, int status, long ticketTypeId, LocalDate visitDate,
                                             int available, int reserved, int sold) {
        Order order = new OrderDAO().findById(orderId).orElseThrow();
        TicketInventory inventory = new TicketInventoryDAO()
                .findByTicketType(ticketTypeId, visitDate, visitDate).get(0);
        assertEquals(status, order.getStatus());
        assertEquals(available, inventory.getAvailableStock());
        assertEquals(reserved, inventory.getReservedStock());
        assertEquals(sold, inventory.getSoldStock());
        assertEquals(inventory.getTotalStock(), inventory.getAvailableStock()
                + inventory.getReservedStock() + inventory.getSoldStock());
        return order;
    }

    private static void forceExpired(long orderId) throws Exception {
        try (Connection connection = MySQLDBUtil.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE orders SET expires_at = DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MINUTE) WHERE order_id = ?")) {
            statement.setLong(1, orderId);
            statement.executeUpdate();
        }
    }

    private static void cleanup(List<Long> orderIds, long ticketTypeId, LocalDate visitDate) throws Exception {
        try (Connection connection = MySQLDBUtil.getConnection()) {
            for (Long orderId : orderIds) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM refunds WHERE order_id = ?")) {
                    statement.setLong(1, orderId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM orders WHERE order_id = ?")) {
                    statement.setLong(1, orderId);
                    statement.executeUpdate();
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM ticket_inventory WHERE ticket_type_id = ? AND visit_date = ?")) {
                statement.setLong(1, ticketTypeId);
                statement.setDate(2, java.sql.Date.valueOf(visitDate));
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM ticket_types WHERE ticket_type_id = ?")) {
                statement.setLong(1, ticketTypeId);
                statement.executeUpdate();
            }
        }
    }
}
