package com.scenicticket.dao.mysql;

import com.scenicticket.dao.BaseDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.TicketInventory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TicketInventoryDAO extends BaseDAO {
    public List<TicketInventory> findByTicketType(long ticketTypeId, LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT * FROM ticket_inventory
                WHERE ticket_type_id = ? AND visit_date BETWEEN ? AND ?
                ORDER BY visit_date
                """;
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketTypeId);
            statement.setDate(2, Date.valueOf(startDate));
            statement.setDate(3, Date.valueOf(endDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TicketInventory> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new DBException("查询每日库存失败", exception);
        }
    }

    public Optional<TicketInventory> findForUpdate(Connection connection, long ticketTypeId,
                                                    LocalDate visitDate) throws SQLException {
        String sql = "SELECT * FROM ticket_inventory WHERE ticket_type_id = ? AND visit_date = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketTypeId);
            statement.setDate(2, Date.valueOf(visitDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    public void insert(Connection connection, long ticketTypeId, LocalDate visitDate, int totalStock)
            throws SQLException {
        String sql = """
                INSERT INTO ticket_inventory
                    (ticket_type_id, visit_date, total_stock, available_stock, reserved_stock, sold_stock)
                VALUES (?, ?, ?, ?, 0, 0)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketTypeId);
            statement.setDate(2, Date.valueOf(visitDate));
            statement.setInt(3, totalStock);
            statement.setInt(4, totalStock);
            statement.executeUpdate();
        }
    }

    public void updateTotal(Connection connection, TicketInventory current, int totalStock) throws SQLException {
        int committed = current.getReservedStock() + current.getSoldStock();
        if (totalStock < committed) {
            throw new BusinessException("总库存不能小于已预留和已售数量之和：" + committed);
        }
        String sql = """
                UPDATE ticket_inventory
                SET total_stock = ?, available_stock = ?, version = version + 1
                WHERE inventory_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, totalStock);
            statement.setInt(2, totalStock - committed);
            statement.setLong(3, current.getInventoryId());
            statement.executeUpdate();
        }
    }

    public TicketInventory reserve(Connection connection, long ticketTypeId, LocalDate visitDate, int quantity)
            throws SQLException {
        TicketInventory current = findForUpdate(connection, ticketTypeId, visitDate)
                .orElseThrow(() -> new BusinessException("该票种在所选日期没有库存计划"));
        if (current.getAvailableStock() < quantity) {
            throw new BusinessException("所选日期库存不足，当前可售 " + current.getAvailableStock() + " 张");
        }
        String sql = """
                UPDATE ticket_inventory
                SET available_stock = available_stock - ?, reserved_stock = reserved_stock + ?, version = version + 1
                WHERE inventory_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setInt(2, quantity);
            statement.setLong(3, current.getInventoryId());
            statement.executeUpdate();
        }
        current.setAvailableStock(current.getAvailableStock() - quantity);
        current.setReservedStock(current.getReservedStock() + quantity);
        current.setVersion(current.getVersion() + 1);
        return current;
    }

    private TicketInventory map(ResultSet resultSet) throws SQLException {
        TicketInventory inventory = new TicketInventory();
        inventory.setInventoryId(resultSet.getLong("inventory_id"));
        inventory.setTicketTypeId(resultSet.getLong("ticket_type_id"));
        inventory.setVisitDate(resultSet.getDate("visit_date").toLocalDate());
        inventory.setTotalStock(resultSet.getInt("total_stock"));
        inventory.setAvailableStock(resultSet.getInt("available_stock"));
        inventory.setReservedStock(resultSet.getInt("reserved_stock"));
        inventory.setSoldStock(resultSet.getInt("sold_stock"));
        inventory.setVersion(resultSet.getInt("version"));
        inventory.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        inventory.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return inventory;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
