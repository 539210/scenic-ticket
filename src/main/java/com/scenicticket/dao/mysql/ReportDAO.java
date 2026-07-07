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
}
