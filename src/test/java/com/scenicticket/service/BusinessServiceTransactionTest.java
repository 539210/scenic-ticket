package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.DetailDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mysql.CategoryDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.Item;
import com.scenicticket.model.Order;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessServiceTransactionTest {
    @Test
    void createOrderCommitsAndWritesActionLogOnSuccess() {
        TrackingConnection trackingConnection = TrackingConnection.create();
        SucceedingOrderDAO orderDAO = new SucceedingOrderDAO(77L);
        CapturingLogDAO logDAO = new CapturingLogDAO();
        BusinessService service = newService(orderDAO, logDAO, trackingConnection);

        long orderId = service.createOrder(1L, 2L, 2, "微信");

        assertEquals(77L, orderId);
        assertSame(trackingConnection.connection, orderDAO.connection);
        assertEquals(List.of(false, true), trackingConnection.autoCommitValues);
        assertTrue(trackingConnection.committed);
        assertFalse(trackingConnection.rolledBack);
        assertTrue(trackingConnection.closed);
        assertEquals("ORDER", logDAO.actionType);
    }

    @Test
    void createOrderRollsBackWhenOrderInsertFails() {
        TrackingConnection trackingConnection = TrackingConnection.create();
        SQLException cause = new SQLException("insert failed");
        FailingOrderDAO orderDAO = new FailingOrderDAO(cause);
        CapturingLogDAO logDAO = new CapturingLogDAO();
        BusinessService service = newService(orderDAO, logDAO, trackingConnection);

        DBException exception = assertThrows(DBException.class,
                () -> service.createOrder(1L, 2L, 2, "微信"));

        assertSame(cause, exception.getCause());
        assertSame(trackingConnection.connection, orderDAO.connection);
        assertEquals(List.of(false, true), trackingConnection.autoCommitValues);
        assertFalse(trackingConnection.committed);
        assertTrue(trackingConnection.rolledBack);
        assertTrue(trackingConnection.closed);
        assertEquals(0, logDAO.writeCount);
    }

    @Test
    void createOrderRejectsInvalidInputBeforeOpeningConnection() {
        TrackingConnection trackingConnection = TrackingConnection.create();
        BusinessService service = newService(new SucceedingOrderDAO(1L), new CapturingLogDAO(), trackingConnection);

        assertThrows(BusinessException.class,
                () -> service.createOrder(0L, 2L, 1, "微信"));
        assertThrows(BusinessException.class,
                () -> service.createOrder(1L, 2L, 0, "微信"));
        assertFalse(trackingConnection.closed);
        assertEquals(List.of(), trackingConnection.autoCommitValues);
    }

    private static BusinessService newService(OrderDAO orderDAO, LogDAO logDAO,
                                              TrackingConnection trackingConnection) {
        return new BusinessService(new CategoryDAO(), new PricingItemDAO(), orderDAO, new DetailDAO(), logDAO,
                new CommentDAO(), () -> trackingConnection.connection);
    }

    private static class PricingItemDAO extends ItemDAO {
        @Override
        public Optional<Item> findById(long itemId) {
            Item item = new Item();
            item.setItemId(itemId);
            item.setTitle("测试景点");
            item.setCategoryId(1L);
            item.setPrice(new BigDecimal("88.00"));
            item.setDiscountRate(BigDecimal.ZERO);
            item.setStatus(1);
            return Optional.of(item);
        }
    }

    private static class SucceedingOrderDAO extends OrderDAO {
        private final long orderId;
        private Connection connection;

        private SucceedingOrderDAO(long orderId) {
            this.orderId = orderId;
        }

        @Override
        public long create(Connection connection, Order order) {
            this.connection = connection;
            assertEquals(1L, order.getUserId());
            assertEquals(2L, order.getItemId());
            assertEquals(new BigDecimal("176.00"), order.getAmount());
            assertEquals(2, order.getQuantity());
            assertEquals(new BigDecimal("88.00"), order.getUnitPrice());
            assertEquals(new BigDecimal("0.00"), order.getDiscountRate());
            assertEquals("微信", order.getPaymentMethod());
            assertEquals(1, order.getStatus());
            return orderId;
        }
    }

    private static class FailingOrderDAO extends OrderDAO {
        private final SQLException exception;
        private Connection connection;

        private FailingOrderDAO(SQLException exception) {
            this.exception = exception;
        }

        @Override
        public long create(Connection connection, Order order) throws SQLException {
            this.connection = connection;
            throw exception;
        }
    }

    private static class CapturingLogDAO extends LogDAO {
        private String actionType;
        private int writeCount;

        @Override
        public void recordAction(long userId, long itemId, String actionType, int durationSeconds,
                                 String clientType, String ip) {
            this.actionType = actionType;
            writeCount += 1;
        }
    }

    private static class TrackingConnection {
        private final List<Boolean> autoCommitValues = new ArrayList<>();
        private Connection connection;
        private boolean committed;
        private boolean rolledBack;
        private boolean closed;

        private static TrackingConnection create() {
            TrackingConnection tracking = new TrackingConnection();
            tracking.connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> tracking.invoke(proxy, method.getName(), args));
            return tracking;
        }

        private Object invoke(Object proxy, String methodName, Object[] args) {
            return switch (methodName) {
                case "setAutoCommit" -> {
                    autoCommitValues.add((Boolean) args[0]);
                    yield null;
                }
                case "commit" -> {
                    committed = true;
                    yield null;
                }
                case "rollback" -> {
                    rolledBack = true;
                    yield null;
                }
                case "close" -> {
                    closed = true;
                    yield null;
                }
                case "isClosed" -> closed;
                case "getAutoCommit" -> true;
                case "toString" -> "TrackingConnection";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("Unexpected connection method: " + methodName);
            };
        }
    }
}
