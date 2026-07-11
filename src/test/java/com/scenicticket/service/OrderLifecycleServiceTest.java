package com.scenicticket.service;

import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dao.mysql.RefundDAO;
import com.scenicticket.dao.mysql.TicketInventoryDAO;
import com.scenicticket.dao.mysql.TicketTypeDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.Item;
import com.scenicticket.model.Order;
import com.scenicticket.model.Refund;
import com.scenicticket.model.TicketInventory;
import com.scenicticket.model.TicketType;
import com.scenicticket.model.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderLifecycleServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-11T04:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Test
    void createsPendingOrderFromServerSnapshotsAndReservesStock() {
        Fixture fixture = new Fixture();

        var result = fixture.service.createPendingOrder(2L, 9L, LocalDate.of(2026, 7, 20), 2, "微信", "127.0.0.1");

        Order order = fixture.orders.created;
        assertEquals(55L, result.orderId());
        assertEquals(OrderLifecycleService.STATUS_PENDING, order.getStatus());
        assertEquals("学生票", order.getTicketTypeNameSnapshot());
        assertEquals(new BigDecimal("100.00"), order.getOriginalUnitPrice());
        assertEquals(new BigDecimal("80.00"), order.getDiscountedUnitPrice());
        assertEquals(new BigDecimal("160.00"), order.getAmount());
        assertEquals(LocalDateTime.of(2026, 7, 11, 12, 15), order.getExpiresAt());
        assertEquals(2, fixture.inventory.reservedQuantity);
        assertTrue(fixture.connection.committed);
    }

    @Test
    void explicitPaymentMovesReservedStockToSold() {
        Fixture fixture = new Fixture();
        fixture.orders.locked = order(0, LocalDateTime.of(2026, 7, 11, 12, 15), LocalDate.of(2026, 7, 20));

        var result = fixture.service.pay(2L, 44L, "127.0.0.1");

        assertTrue(result.updated());
        assertEquals(2, fixture.inventory.confirmedQuantity);
        assertTrue(fixture.orders.paid);
        assertTrue(fixture.connection.committed);
    }

    @Test
    void expiredPaymentCancelsOrderAndReleasesReservation() {
        Fixture fixture = new Fixture();
        fixture.orders.locked = order(0, LocalDateTime.of(2026, 7, 11, 11, 59), LocalDate.of(2026, 7, 20));

        var result = fixture.service.pay(2L, 44L, "127.0.0.1");

        assertTrue(result.message().contains("已过期"));
        assertEquals(2, fixture.inventory.releasedQuantity);
        assertTrue(fixture.orders.cancelled);
        assertFalse(fixture.orders.paid);
    }

    @Test
    void illegalTransitionRollsBackWithoutChangingStock() {
        Fixture fixture = new Fixture();
        fixture.orders.locked = order(1, null, LocalDate.of(2026, 7, 20));

        assertThrows(BusinessException.class,
                () -> fixture.service.cancelPending(2L, 44L, "127.0.0.1"));

        assertEquals(0, fixture.inventory.releasedQuantity);
        assertTrue(fixture.connection.rolledBack);
    }

    @Test
    void refundWritesRecordAndRestoresSoldStockInSameTransaction() {
        Fixture fixture = new Fixture();
        fixture.orders.locked = order(1, null, LocalDate.of(2026, 7, 20));

        var result = fixture.service.refund(2L, 44L, "行程取消", "127.0.0.1");

        assertTrue(result.updated());
        assertEquals(2, fixture.inventory.restoredQuantity);
        assertEquals(new BigDecimal("160.00"), fixture.refunds.created.getRefundAmount());
        assertEquals("行程取消", fixture.refunds.created.getReason());
        assertTrue(fixture.orders.refunded);
        assertTrue(fixture.connection.committed);
    }

    @Test
    void mongoAuditFailureDoesNotUndoCommittedMysqlPayment() {
        Fixture fixture = new Fixture();
        fixture.logs.fail = true;
        fixture.orders.locked = order(0, LocalDateTime.of(2026, 7, 11, 12, 15), LocalDate.of(2026, 7, 20));

        var result = fixture.service.pay(2L, 44L, "127.0.0.1");

        assertFalse(result.auditRecorded());
        assertTrue(result.message().contains("审计日志写入失败"));
        assertTrue(fixture.orders.paid);
        assertTrue(fixture.connection.committed);
    }

    @Test
    void refundRejectsExpiredCompletedAndAdmittedOrdersWithoutRestoringStock() {
        Fixture expired = new Fixture();
        expired.orders.locked = order(1, null, LocalDate.of(2026, 7, 11));
        assertThrows(BusinessException.class,
                () -> expired.service.refund(2L, 44L, "日期已到", "127.0.0.1"));
        assertEquals(0, expired.inventory.restoredQuantity);

        Fixture completed = new Fixture();
        completed.orders.locked = order(3, null, LocalDate.of(2026, 7, 20));
        assertThrows(BusinessException.class,
                () -> completed.service.refund(2L, 44L, "已经完成", "127.0.0.1"));
        assertEquals(0, completed.inventory.restoredQuantity);

        Fixture admitted = new Fixture();
        admitted.orders.locked = order(1, null, LocalDate.of(2026, 7, 20));
        admitted.orders.hasAdmission = true;
        assertThrows(BusinessException.class,
                () -> admitted.service.refund(2L, 44L, "已经入园", "127.0.0.1"));
        assertEquals(0, admitted.inventory.restoredQuantity);
    }

    private static Order order(int status, LocalDateTime expiresAt, LocalDate visitDate) {
        Order order = new Order();
        order.setOrderId(44L);
        order.setUserId(2L);
        order.setItemId(7L);
        order.setTicketTypeId(9L);
        order.setVisitDate(visitDate);
        order.setQuantity(2);
        order.setAmount(new BigDecimal("160.00"));
        order.setStatus(status);
        order.setExpiresAt(expiresAt);
        return order;
    }

    private static class Fixture {
        private final FakeOrderDAO orders = new FakeOrderDAO();
        private final FakeInventoryDAO inventory = new FakeInventoryDAO();
        private final FakeRefundDAO refunds = new FakeRefundDAO();
        private final FakeLogDAO logs = new FakeLogDAO();
        private final TrackingConnection connection = TrackingConnection.create();
        private final OrderLifecycleService service = new OrderLifecycleService(orders, new FakeTicketTypeDAO(),
                inventory, new FakeItemDAO(), refunds, logs, authorization(), () -> connection.connection, CLOCK);
    }

    private static AuthorizationService authorization() {
        return new AuthorizationService() {
            @Override
            public User requireActiveUser(long actorUserId) {
                User user = new User();
                user.setUserId(actorUserId);
                user.setRole("USER");
                user.setStatus(1);
                return user;
            }
        };
    }

    private static class FakeTicketTypeDAO extends TicketTypeDAO {
        @Override
        public Optional<TicketType> findById(Connection connection, long ticketTypeId) {
            TicketType type = new TicketType();
            type.setTicketTypeId(ticketTypeId);
            type.setItemId(7L);
            type.setName("学生票");
            type.setOriginalPrice(new BigDecimal("100.00"));
            type.setDiscountRate(new BigDecimal("20.00"));
            type.setStatus(1);
            return Optional.of(type);
        }
    }

    private static class FakeItemDAO extends ItemDAO {
        @Override
        public Optional<Item> findById(Connection connection, long itemId) {
            Item item = new Item();
            item.setItemId(itemId);
            item.setStatus(1);
            return Optional.of(item);
        }
    }

    private static class FakeInventoryDAO extends TicketInventoryDAO {
        private int reservedQuantity;
        private int confirmedQuantity;
        private int releasedQuantity;
        private int restoredQuantity;

        @Override
        public TicketInventory reserve(Connection connection, long typeId, LocalDate date, int quantity) {
            reservedQuantity += quantity;
            return new TicketInventory();
        }
        @Override
        public TicketInventory confirmSale(Connection connection, long typeId, LocalDate date, int quantity) {
            confirmedQuantity += quantity;
            return new TicketInventory();
        }
        @Override
        public TicketInventory releaseReservation(Connection connection, long typeId, LocalDate date, int quantity) {
            releasedQuantity += quantity;
            return new TicketInventory();
        }
        @Override
        public boolean releaseReservationIfPresent(Connection connection, long typeId, LocalDate date, int quantity) {
            releasedQuantity += quantity;
            return true;
        }
        @Override
        public TicketInventory restoreSold(Connection connection, long typeId, LocalDate date, int quantity) {
            restoredQuantity += quantity;
            return new TicketInventory();
        }
    }

    private static class FakeOrderDAO extends OrderDAO {
        private Order created;
        private Order locked;
        private boolean paid;
        private boolean cancelled;
        private boolean refunded;
        private boolean hasAdmission;

        @Override
        public long create(Connection connection, Order order) {
            created = order;
            return 55L;
        }
        @Override
        public Optional<Order> findByIdForUpdate(Connection connection, long orderId) {
            return Optional.ofNullable(locked);
        }
        @Override
        public boolean markPaid(Connection connection, long orderId) {
            paid = true;
            return true;
        }
        @Override
        public boolean markCancelled(Connection connection, long orderId, int expectedStatus, boolean isRefunded) {
            cancelled = true;
            refunded = isRefunded;
            return true;
        }
        @Override
        public boolean hasAdmissions(Connection connection, long orderId) { return hasAdmission; }
    }

    private static class FakeRefundDAO extends RefundDAO {
        private Refund created;
        @Override
        public Optional<Refund> findByOrderId(Connection connection, long orderId) { return Optional.empty(); }
        @Override
        public long create(Connection connection, Refund refund) { created = refund; return 8L; }
    }

    private static class FakeLogDAO extends LogDAO {
        private boolean fail;
        @Override
        public void recordAction(long userId, long itemId, String actionType, int durationSeconds,
                                 String clientType, String ip) {
            if (fail) throw new IllegalStateException("Mongo unavailable");
        }
    }

    private static class TrackingConnection {
        private final List<Boolean> autoCommitValues = new ArrayList<>();
        private Connection connection;
        private boolean committed;
        private boolean rolledBack;

        private static TrackingConnection create() {
            TrackingConnection tracking = new TrackingConnection();
            tracking.connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "setAutoCommit" -> { tracking.autoCommitValues.add((Boolean) args[0]); yield null; }
                        case "commit" -> { tracking.committed = true; yield null; }
                        case "rollback" -> { tracking.rolledBack = true; yield null; }
                        case "close" -> null;
                        case "isClosed" -> false;
                        case "unwrap" -> null;
                        case "isWrapperFor" -> false;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            return tracking;
        }
    }
}
