package com.scenicticket.dao.mysql;

import com.scenicticket.dao.BaseDAO;
import com.scenicticket.exception.DBException;
import com.scenicticket.dto.OrderViewDTO;
import com.scenicticket.dto.UserOrderSummaryDTO;
import com.scenicticket.model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDAO extends BaseDAO {
    public long create(Order order) {
        try (Connection connection = getConnection()) {
            return create(connection, order);
        } catch (SQLException e) {
            throw new DBException("Failed to create order.", e);
        }
    }

    public long create(Connection connection, Order order) throws SQLException {
        String sql = """
                INSERT INTO orders (
                    user_id, item_id, amount, quantity, unit_price, discount_rate, payment_method,
                    ticket_type_id, ticket_type_name_snapshot, original_unit_price, discounted_unit_price,
                    visit_date, expires_at, status_version, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, order.getUserId());
            statement.setLong(2, order.getItemId());
            statement.setBigDecimal(3, order.getAmount());
            statement.setInt(4, order.getQuantity());
            statement.setBigDecimal(5, order.getUnitPrice());
            statement.setBigDecimal(6, order.getDiscountRate());
            statement.setString(7, order.getPaymentMethod());
            statement.setLong(8, order.getTicketTypeId());
            statement.setString(9, order.getTicketTypeNameSnapshot());
            statement.setBigDecimal(10, order.getOriginalUnitPrice());
            statement.setBigDecimal(11, order.getDiscountedUnitPrice());
            statement.setDate(12, java.sql.Date.valueOf(order.getVisitDate()));
            statement.setTimestamp(13, Timestamp.valueOf(order.getExpiresAt()));
            statement.setInt(14, order.getStatusVersion() == null ? 0 : order.getStatusVersion());
            statement.setInt(15, order.getStatus() == null ? 0 : order.getStatus());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            throw new DBException("Failed to read generated order id.");
        }
    }

    public Optional<Order> findById(long orderId) {
        String sql = """
                SELECT order_id, user_id, item_id, amount, quantity, unit_price, discount_rate, payment_method,
                       ticket_type_id, ticket_type_name_snapshot, original_unit_price, discounted_unit_price,
                       visit_date, expires_at, paid_at, cancelled_at, completed_at, refunded_at,
                       status_version, status, created_at
                FROM orders
                WHERE order_id = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapOrder(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DBException("Failed to find order by id.", e);
        }
    }

    public Optional<Order> findByIdForUpdate(Connection connection, long orderId) throws SQLException {
        String sql = """
                SELECT order_id, user_id, item_id, amount, quantity, unit_price, discount_rate, payment_method,
                       ticket_type_id, ticket_type_name_snapshot, original_unit_price, discounted_unit_price,
                       visit_date, expires_at, paid_at, cancelled_at, completed_at, refunded_at,
                       status_version, status, created_at
                FROM orders WHERE order_id = ? FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapOrder(resultSet)) : Optional.empty();
            }
        }
    }

    public List<Order> findByUserId(long userId, int limit, int offset) {
        String sql = """
                SELECT order_id, user_id, item_id, amount, quantity, unit_price, discount_rate, payment_method,
                       ticket_type_id, ticket_type_name_snapshot, original_unit_price, discounted_unit_price,
                       visit_date, expires_at, paid_at, cancelled_at, completed_at, refunded_at,
                       status_version, status, created_at
                FROM orders
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setInt(2, limit);
            statement.setInt(3, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Order> orders = new ArrayList<>();
                while (resultSet.next()) {
                    orders.add(mapOrder(resultSet));
                }
                return orders;
            }
        } catch (SQLException e) {
            throw new DBException("Failed to list user orders.", e);
        }
    }

    public List<Order> search(Long userId, Long orderId, Integer status, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT order_id, user_id, item_id, amount, quantity, unit_price, discount_rate, payment_method,
                       ticket_type_id, ticket_type_name_snapshot, original_unit_price, discounted_unit_price,
                       visit_date, expires_at, paid_at, cancelled_at, completed_at, refunded_at,
                       status_version, status, created_at
                FROM orders
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();
        if (userId != null) {
            sql.append(" AND user_id = ?");
            parameters.add(userId);
        }
        if (orderId != null) {
            sql.append(" AND order_id = ?");
            parameters.add(orderId);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            parameters.add(status);
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            for (Object parameter : parameters) {
                if (parameter instanceof Long longValue) {
                    statement.setLong(parameterIndex, longValue);
                } else if (parameter instanceof Integer intValue) {
                    statement.setInt(parameterIndex, intValue);
                }
                parameterIndex += 1;
            }
            statement.setInt(parameterIndex, limit);
            statement.setInt(parameterIndex + 1, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Order> orders = new ArrayList<>();
                while (resultSet.next()) {
                    orders.add(mapOrder(resultSet));
                }
                return orders;
            }
        } catch (SQLException e) {
            throw new DBException("Failed to search orders.", e);
        }
    }

    public List<OrderViewDTO> searchViews(Long userId, Long orderId, Integer status, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT o.order_id, o.user_id, o.item_id, o.amount, o.quantity, o.unit_price,
                       o.discount_rate, o.payment_method, o.ticket_type_id, o.ticket_type_name_snapshot,
                       o.original_unit_price, o.discounted_unit_price, o.visit_date, o.expires_at,
                       o.paid_at, o.cancelled_at, o.completed_at, o.refunded_at,
                       o.status_version, o.status, o.created_at,
                       COALESCE(i.title, '景点已删除') AS item_title
                FROM orders o
                LEFT JOIN items i ON i.item_id = o.item_id
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();
        if (userId != null) {
            sql.append(" AND o.user_id = ?");
            parameters.add(userId);
        }
        if (orderId != null) {
            sql.append(" AND o.order_id = ?");
            parameters.add(orderId);
        }
        if (status != null) {
            sql.append(" AND o.status = ?");
            parameters.add(status);
        }
        sql.append(" ORDER BY o.created_at DESC LIMIT ? OFFSET ?");

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameterIndex = 1;
            for (Object parameter : parameters) {
                if (parameter instanceof Long longValue) {
                    statement.setLong(parameterIndex, longValue);
                } else if (parameter instanceof Integer intValue) {
                    statement.setInt(parameterIndex, intValue);
                }
                parameterIndex += 1;
            }
            statement.setInt(parameterIndex, limit);
            statement.setInt(parameterIndex + 1, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<OrderViewDTO> orders = new ArrayList<>();
                while (resultSet.next()) {
                    OrderViewDTO view = new OrderViewDTO();
                    view.setOrder(mapOrder(resultSet));
                    view.setItemTitle(resultSet.getString("item_title"));
                    orders.add(view);
                }
                return orders;
            }
        } catch (SQLException e) {
            throw new DBException("Failed to search order views.", e);
        }
    }

    public boolean updateStatus(long orderId, int status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, status);
            statement.setLong(2, orderId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DBException("Failed to update order status.", e);
        }
    }

    public boolean markPaid(Connection connection, long orderId) throws SQLException {
        return transition(connection, orderId, 0, 1, "paid_at");
    }

    public boolean markCancelled(Connection connection, long orderId, int expectedStatus, boolean refunded)
            throws SQLException {
        String timestamps = refunded
                ? "refunded_at = CURRENT_TIMESTAMP, cancelled_at = CURRENT_TIMESTAMP"
                : "cancelled_at = CURRENT_TIMESTAMP";
        String sql = "UPDATE orders SET status = 2, " + timestamps
                + ", status_version = status_version + 1 WHERE order_id = ? AND status = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setInt(2, expectedStatus);
            return statement.executeUpdate() == 1;
        }
    }

    public List<Long> findExpiredPendingIds(int limit) {
        String sql = """
                SELECT order_id FROM orders
                WHERE status = 0 AND expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP
                ORDER BY expires_at LIMIT ?
                """;
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong(1));
                }
                return ids;
            }
        } catch (SQLException exception) {
            throw new DBException("查询过期待支付订单失败", exception);
        }
    }

    public boolean hasAdmissions(Connection connection, long orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM admissions WHERE order_id = ? LIMIT 1")) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean transition(Connection connection, long orderId, int expectedStatus, int targetStatus,
                               String timeColumn) throws SQLException {
        String sql = "UPDATE orders SET status = ?, " + timeColumn
                + " = CURRENT_TIMESTAMP, status_version = status_version + 1 WHERE order_id = ? AND status = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, targetStatus);
            statement.setLong(2, orderId);
            statement.setInt(3, expectedStatus);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean existsPaidOrder(long userId, long itemId) {
        String sql = """
                SELECT 1
                FROM orders
                WHERE user_id = ?
                  AND item_id = ?
                  AND status IN (1, 3)
                LIMIT 1
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DBException("Failed to check paid order.", e);
        }
    }

    public UserOrderSummaryDTO summarizeByUserId(long userId) {
        String sql = """
                SELECT COUNT(*) AS total_orders,
                       SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS pending_orders,
                       SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS paid_orders,
                       SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS cancelled_orders,
                       SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS completed_orders,
                       COALESCE(SUM(CASE WHEN status IN (1, 3) THEN amount ELSE 0 END), 0) AS paid_amount
                FROM orders
                WHERE user_id = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                UserOrderSummaryDTO summary = new UserOrderSummaryDTO();
                if (resultSet.next()) {
                    summary.setTotalOrders(resultSet.getLong("total_orders"));
                    summary.setPendingOrders(resultSet.getLong("pending_orders"));
                    summary.setPaidOrders(resultSet.getLong("paid_orders"));
                    summary.setCancelledOrders(resultSet.getLong("cancelled_orders"));
                    summary.setCompletedOrders(resultSet.getLong("completed_orders"));
                    summary.setPaidAmount(resultSet.getBigDecimal("paid_amount"));
                }
                return summary;
            }
        } catch (SQLException e) {
            throw new DBException("Failed to summarize user orders.", e);
        }
    }

    private Order mapOrder(ResultSet resultSet) throws SQLException {
        Order order = new Order();
        order.setOrderId(resultSet.getLong("order_id"));
        order.setUserId(resultSet.getLong("user_id"));
        order.setItemId(resultSet.getLong("item_id"));
        order.setAmount(resultSet.getBigDecimal("amount"));
        order.setQuantity(resultSet.getInt("quantity"));
        order.setUnitPrice(resultSet.getBigDecimal("unit_price"));
        order.setDiscountRate(resultSet.getBigDecimal("discount_rate"));
        order.setPaymentMethod(resultSet.getString("payment_method"));
        long ticketTypeId = resultSet.getLong("ticket_type_id");
        order.setTicketTypeId(resultSet.wasNull() ? null : ticketTypeId);
        order.setTicketTypeNameSnapshot(resultSet.getString("ticket_type_name_snapshot"));
        order.setOriginalUnitPrice(resultSet.getBigDecimal("original_unit_price"));
        order.setDiscountedUnitPrice(resultSet.getBigDecimal("discounted_unit_price"));
        java.sql.Date visitDate = resultSet.getDate("visit_date");
        order.setVisitDate(visitDate == null ? null : visitDate.toLocalDate());
        order.setExpiresAt(toLocalDateTime(resultSet.getTimestamp("expires_at")));
        order.setPaidAt(toLocalDateTime(resultSet.getTimestamp("paid_at")));
        order.setCancelledAt(toLocalDateTime(resultSet.getTimestamp("cancelled_at")));
        order.setCompletedAt(toLocalDateTime(resultSet.getTimestamp("completed_at")));
        order.setRefundedAt(toLocalDateTime(resultSet.getTimestamp("refunded_at")));
        order.setStatusVersion(resultSet.getInt("status_version"));
        order.setStatus(resultSet.getInt("status"));
        order.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        return order;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
