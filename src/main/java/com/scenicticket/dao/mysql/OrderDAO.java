package com.scenicticket.dao.mysql;

import com.scenicticket.dao.BaseDAO;
import com.scenicticket.exception.DBException;
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
        String sql = "INSERT INTO orders (user_id, item_id, amount, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, order.getUserId());
            statement.setLong(2, order.getItemId());
            statement.setBigDecimal(3, order.getAmount());
            statement.setInt(4, order.getStatus() == null ? 0 : order.getStatus());
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
                SELECT order_id, user_id, item_id, amount, status, created_at
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

    public List<Order> findByUserId(long userId, int limit, int offset) {
        String sql = """
                SELECT order_id, user_id, item_id, amount, status, created_at
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

    private Order mapOrder(ResultSet resultSet) throws SQLException {
        Order order = new Order();
        order.setOrderId(resultSet.getLong("order_id"));
        order.setUserId(resultSet.getLong("user_id"));
        order.setItemId(resultSet.getLong("item_id"));
        order.setAmount(resultSet.getBigDecimal("amount"));
        order.setStatus(resultSet.getInt("status"));
        order.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        return order;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
