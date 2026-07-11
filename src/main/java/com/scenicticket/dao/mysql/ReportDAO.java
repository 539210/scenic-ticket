package com.scenicticket.dao.mysql;

import com.scenicticket.dao.BaseDAO;
import com.scenicticket.dto.MonthlyOrderReportDTO;
import com.scenicticket.exception.DBException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
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
