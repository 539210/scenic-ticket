package com.scenicticket.dao.mysql;

import com.scenicticket.dao.BaseDAO;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.TicketType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TicketTypeDAO extends BaseDAO {
    public long create(TicketType ticketType) {
        String sql = "INSERT INTO ticket_types (item_id, name, original_price, discount_rate, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, ticketType.getItemId());
            statement.setString(2, ticketType.getName());
            statement.setBigDecimal(3, ticketType.getOriginalPrice());
            statement.setBigDecimal(4, ticketType.getDiscountRate());
            statement.setInt(5, ticketType.getStatus());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new DBException("创建票种后未返回编号");
        } catch (SQLException exception) {
            throw new DBException("创建票种失败", exception);
        }
    }

    public Optional<TicketType> findById(long ticketTypeId) {
        try (Connection connection = getConnection()) {
            return findById(connection, ticketTypeId);
        } catch (SQLException exception) {
            throw new DBException("查询票种失败", exception);
        }
    }

    public Optional<TicketType> findById(Connection connection, long ticketTypeId) throws SQLException {
        String sql = "SELECT * FROM ticket_types WHERE ticket_type_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketTypeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public List<TicketType> findByItemId(long itemId, boolean activeOnly) {
        String sql = "SELECT * FROM ticket_types WHERE item_id = ?"
                + (activeOnly ? " AND status = 1" : "") + " ORDER BY ticket_type_id";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TicketType> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new DBException("查询景点票种失败", exception);
        }
    }

    public boolean update(TicketType ticketType) {
        String sql = """
                UPDATE ticket_types
                SET name = ?, original_price = ?, discount_rate = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE ticket_type_id = ?
                """;
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ticketType.getName());
            statement.setBigDecimal(2, ticketType.getOriginalPrice());
            statement.setBigDecimal(3, ticketType.getDiscountRate());
            statement.setInt(4, ticketType.getStatus());
            statement.setLong(5, ticketType.getTicketTypeId());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DBException("更新票种失败", exception);
        }
    }

    private TicketType map(ResultSet resultSet) throws SQLException {
        TicketType ticketType = new TicketType();
        ticketType.setTicketTypeId(resultSet.getLong("ticket_type_id"));
        ticketType.setItemId(resultSet.getLong("item_id"));
        ticketType.setName(resultSet.getString("name"));
        ticketType.setOriginalPrice(resultSet.getBigDecimal("original_price"));
        ticketType.setDiscountRate(resultSet.getBigDecimal("discount_rate"));
        ticketType.setStatus(resultSet.getInt("status"));
        ticketType.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        ticketType.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return ticketType;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
