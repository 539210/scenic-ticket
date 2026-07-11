package com.scenicticket.integration;

import com.scenicticket.config.DBConfig;
import com.scenicticket.config.DatabaseTargetGuard;
import com.scenicticket.dao.mysql.AdmissionDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.service.AdmissionService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdmissionIntegrationTest {
    @BeforeAll
    static void verifyTargets() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("integrationTests", "false")),
                "Set -DintegrationTests=true with explicit scenic_ticket_test overrides.");
        DatabaseTargetGuard.requireIsolatedTestTargets(DBConfig.get("mysql.url"), DBConfig.get("mongodb.database"));
    }

    @Test
    void partialAndFullAdmissionCompleteOrderAndBlockRefund() throws Exception {
        TicketInventoryService inventoryService = new TicketInventoryService();
        OrderLifecycleService lifecycle = new OrderLifecycleService();
        AdmissionService admissionService = new AdmissionService();
        LocalDate visitDate = LocalDate.now();
        long ticketTypeId = inventoryService.createTicketType(1L, 1L,
                "核销票-" + UUID.randomUUID().toString().substring(0, 8),
                new BigDecimal("50.00"), BigDecimal.ZERO);
        long orderId = 0;
        try {
            inventoryService.setTotalStock(1L, ticketTypeId, visitDate, 5);
            orderId = lifecycle.createPendingOrder(2L, ticketTypeId, visitDate, 3,
                    "微信", "127.0.0.1").orderId();
            lifecycle.pay(2L, orderId, "127.0.0.1");

            var partial = admissionService.admit(1L, orderId, 1, "第一位游客入园");
            assertEquals(1, partial.admittedQuantity());
            assertEquals(2, partial.remainingQuantity());
            assertTrue(!partial.completed());
            assertEquals(1, new OrderDAO().findById(orderId).orElseThrow().getStatus());
            long paidOrderId = orderId;
            assertThrows(BusinessException.class,
                    () -> lifecycle.refund(2L, paidOrderId, "已部分核销仍尝试退款", "127.0.0.1"));

            var completed = admissionService.admit(1L, orderId, 2, "其余游客入园");
            assertTrue(completed.completed());
            assertEquals(3, completed.admittedQuantity());
            assertEquals(0, completed.remainingQuantity());
            var order = new OrderDAO().findById(orderId).orElseThrow();
            assertEquals(3, order.getStatus());
            assertNotNull(order.getCompletedAt());
            assertEquals(2, new AdmissionDAO().findByOrderId(orderId).size());
            assertThrows(BusinessException.class,
                    () -> lifecycle.refund(2L, paidOrderId, "完成后尝试退款", "127.0.0.1"));
        } finally {
            cleanup(orderId, ticketTypeId, visitDate);
        }
    }

    private static void cleanup(long orderId, long ticketTypeId, LocalDate visitDate) throws Exception {
        try (Connection connection = MySQLDBUtil.getConnection()) {
            if (orderId > 0) {
                for (String table : new String[]{"admissions", "refunds", "orders"}) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM " + table + " WHERE order_id = ?")) {
                        statement.setLong(1, orderId);
                        statement.executeUpdate();
                    }
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
