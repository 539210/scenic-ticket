package com.scenicticket.dao.mysql;

import com.scenicticket.dao.BaseDAO;
import com.scenicticket.dto.MonthlyOrderDetailDTO;
import com.scenicticket.dto.MonthlyOrderReportDTO;
import com.scenicticket.exception.DBException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO extends BaseDAO {
    public List<MonthlyOrderReportDTO> callMonthlyOrderReport(int year, int month) {
        String sql = "{CALL sp_monthly_order_report(?, ?)}";
        try (Connection connection = getConnection();
             CallableStatement statement = connection.prepareCall(sql)) {
            statement.setInt(1, year);
            statement.setInt(2, month);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MonthlyOrderReportDTO> reports = new ArrayList<>();
                while (resultSet.next()) {
                    reports.add(mapMonthlyOrderReport(resultSet));
                }
                return reports;
            }
        } catch (SQLException e) {
            throw new DBException("Failed to call monthly order report procedure.", e);
        }
    }

    public List<MonthlyOrderDetailDTO> findMonthlyOrderDetails(LocalDate orderDate) {
        String sql = """
                SELECT o.order_id, o.user_id, u.username, o.item_id,
                       COALESCE(i.title, '景点已删除') AS item_title,
                       COALESCE(o.ticket_type_name_snapshot, '成人票') AS ticket_type_name,
                       o.quantity, COALESCE(o.discounted_unit_price, o.unit_price) AS unit_price,
                       o.amount, o.payment_method, o.status, o.visit_date, o.created_at
                FROM orders o
                JOIN users u ON u.user_id = o.user_id
                LEFT JOIN items i ON i.item_id = o.item_id
                WHERE o.created_at >= ?
                  AND o.created_at < ?
                  AND o.status IN (1, 3)
                ORDER BY o.created_at, o.order_id
                """;
        try (Connection connection = getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(orderDate));
            statement.setDate(2, Date.valueOf(orderDate.plusDays(1)));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MonthlyOrderDetailDTO> details = new ArrayList<>();
                while (resultSet.next()) {
                    Date visitDate = resultSet.getDate("visit_date");
                    var createdAt = resultSet.getTimestamp("created_at");
                    details.add(new MonthlyOrderDetailDTO(
                            resultSet.getLong("order_id"),
                            resultSet.getLong("user_id"),
                            resultSet.getString("username"),
                            resultSet.getLong("item_id"),
                            resultSet.getString("item_title"),
                            resultSet.getString("ticket_type_name"),
                            resultSet.getInt("quantity"),
                            resultSet.getBigDecimal("unit_price"),
                            resultSet.getBigDecimal("amount"),
                            resultSet.getString("payment_method"),
                            resultSet.getInt("status"),
                            visitDate == null ? null : visitDate.toLocalDate(),
                            createdAt == null ? null : createdAt.toLocalDateTime()));
                }
                return details;
            }
        } catch (SQLException e) {
            throw new DBException("Failed to query monthly order details.", e);
        }
    }

    public int callUpdateInactiveItems(int daysWithoutOrders) {
        try (Connection connection = getConnection()) {
            return callUpdateInactiveItems(connection, daysWithoutOrders);
        } catch (SQLException e) {
            throw new DBException("Failed to call inactive item procedure.", e);
        }
    }

    public int callUpdateInactiveItems(Connection connection, int daysWithoutOrders) throws SQLException {
        String sql = "{CALL sp_update_inactive_items(?)}";
        try (CallableStatement statement = connection.prepareCall(sql)) {
            statement.setInt(1, daysWithoutOrders);
            boolean hasResultSet = statement.execute();
            if (hasResultSet) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    }
                }
            }
            return Math.max(statement.getUpdateCount(), 0);
        }
    }

    public int countUserProfileViewRows() {
        try (Connection connection = getConnection()) {
            return countRows(connection, "v_user_profile");
        } catch (SQLException e) {
            throw new DBException("Failed to query user profile view.", e);
        }
    }

    public int countItemOrderSummaryViewRows() {
        try (Connection connection = getConnection()) {
            return countRows(connection, "v_item_order_summary");
        } catch (SQLException e) {
            throw new DBException("Failed to query item order summary view.", e);
        }
    }

    public int countUserProfileViewRows(Connection connection) throws SQLException {
        return countRows(connection, "v_user_profile");
    }

    public int countItemOrderSummaryViewRows(Connection connection) throws SQLException {
        return countRows(connection, "v_item_order_summary");
    }

    private MonthlyOrderReportDTO mapMonthlyOrderReport(ResultSet resultSet) throws SQLException {
        MonthlyOrderReportDTO report = new MonthlyOrderReportDTO();
        Date orderDate = resultSet.getDate("order_date");
        if (orderDate != null) {
            report.setOrderDate(orderDate.toLocalDate());
        }
        report.setOrderCount(resultSet.getInt("order_count"));
        report.setTotalAmount(resultSet.getBigDecimal("total_amount"));
        return report;
    }

    private int countRows(Connection connection, String viewName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + viewName;
        try (var statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }
}
