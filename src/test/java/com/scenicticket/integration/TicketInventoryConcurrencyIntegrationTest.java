package com.scenicticket.integration;

import com.scenicticket.config.DBConfig;
import com.scenicticket.config.DatabaseTargetGuard;
import com.scenicticket.dao.mysql.TicketInventoryDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.TicketInventory;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketInventoryConcurrencyIntegrationTest {
    @BeforeAll
    static void verifyTargets() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("integrationTests", "false")),
                "Set -DintegrationTests=true with explicit scenic_ticket_test overrides.");
        DatabaseTargetGuard.requireIsolatedTestTargets(DBConfig.get("mysql.url"), DBConfig.get("mongodb.database"));
    }

    @Test
    void rowLocksPreventOversellingUnderConcurrentReservations() throws Exception {
        TicketInventoryService service = new TicketInventoryService();
        String name = "并发票-" + UUID.randomUUID().toString().substring(0, 8);
        LocalDate visitDate = LocalDate.now().plusDays(400);
        long ticketTypeId = service.createTicketType(1L, 1L, name,
                new BigDecimal("66.00"), BigDecimal.ZERO);
        try {
            assertTrue(service.updateTicketType(1L, ticketTypeId, name + "-已更新",
                    new BigDecimal("60.00"), new BigDecimal("10.00"), 1));
            service.setTotalStock(1L, ticketTypeId, visitDate, 10);
            var availability = service.listAvailable(2L, 1L, visitDate, visitDate);
            assertEquals(1, availability.stream()
                    .filter(option -> option.ticketType().getTicketTypeId() == ticketTypeId).count());
            assertEquals(new BigDecimal("54.00"), availability.stream()
                    .filter(option -> option.ticketType().getTicketTypeId() == ticketTypeId)
                    .findFirst().orElseThrow().discountedPrice());
            int attempts = 20;
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(attempts);
            try {
                List<Future<Boolean>> futures = new ArrayList<>();
                for (int index = 0; index < attempts; index += 1) {
                    futures.add(executor.submit(() -> reserveOne(service, ticketTypeId, visitDate, start)));
                }
                start.countDown();
                int successes = 0;
                for (Future<Boolean> future : futures) {
                    if (future.get()) {
                        successes += 1;
                    }
                }
                assertEquals(10, successes);
            } finally {
                executor.shutdownNow();
            }

            TicketInventory finalInventory = new TicketInventoryDAO()
                    .findByTicketType(ticketTypeId, visitDate, visitDate).get(0);
            assertEquals(0, finalInventory.getAvailableStock());
            assertEquals(10, finalInventory.getReservedStock());
            assertTrue(finalInventory.getAvailableStock() >= 0);
            assertEquals(finalInventory.getTotalStock(), finalInventory.getAvailableStock()
                    + finalInventory.getReservedStock() + finalInventory.getSoldStock());
        } finally {
            cleanup(ticketTypeId, visitDate);
        }
    }

    private static boolean reserveOne(TicketInventoryService service, long ticketTypeId, LocalDate visitDate,
                                      CountDownLatch start) throws Exception {
        try (Connection connection = MySQLDBUtil.getConnection()) {
            connection.setAutoCommit(false);
            start.await();
            try {
                service.reserveStock(connection, ticketTypeId, visitDate, 1);
                connection.commit();
                return true;
            } catch (BusinessException exception) {
                connection.rollback();
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private static void cleanup(long ticketTypeId, LocalDate visitDate) throws Exception {
        try (Connection connection = MySQLDBUtil.getConnection()) {
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
