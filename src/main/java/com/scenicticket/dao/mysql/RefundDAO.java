package com.scenicticket.dao.mysql;

import com.scenicticket.dao.BaseDAO;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.Refund;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class RefundDAO extends BaseDAO {
    public long create(Connection connection, Refund refund) throws SQLException {
        String sql = """
                INSERT INTO refunds (order_id, refund_amount, reason, status, operator_user_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, refund.getOrderId());
            statement.setBigDecimal(2, refund.getRefundAmount());
            statement.setString(3, refund.getReason());
            statement.setString(4, refund.getRefundStatus());
            statement.setLong(5, refund.getOperatorUserId());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new DBException("退款记录未返回编号");
        }
    }

    public Optional<Refund> findByOrderId(Connection connection, long orderId) throws SQLException {
        String sql = "SELECT * FROM refunds WHERE order_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                Refund refund = new Refund();
                refund.setRefundId(resultSet.getLong("refund_id"));
                refund.setOrderId(resultSet.getLong("order_id"));
                refund.setRefundAmount(resultSet.getBigDecimal("refund_amount"));
                refund.setReason(resultSet.getString("reason"));
                refund.setRefundStatus(resultSet.getString("status"));
                long operatorUserId = resultSet.getLong("operator_user_id");
                refund.setOperatorUserId(resultSet.wasNull() ? null : operatorUserId);
                Timestamp refundedAt = resultSet.getTimestamp("refunded_at");
                refund.setRefundedAt(refundedAt == null ? null : refundedAt.toLocalDateTime());
                return Optional.of(refund);
            }
        }
    }
}
