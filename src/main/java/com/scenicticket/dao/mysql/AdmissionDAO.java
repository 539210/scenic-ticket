package com.scenicticket.dao.mysql;

import com.scenicticket.dao.BaseDAO;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.Admission;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AdmissionDAO extends BaseDAO {
    public long create(Connection connection, Admission admission) throws SQLException {
        String sql = "INSERT INTO admissions (order_id, quantity, operator_user_id, note) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, admission.getOrderId());
            statement.setInt(2, admission.getQuantity());
            statement.setLong(3, admission.getOperatorUserId());
            statement.setString(4, admission.getNote());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new DBException("核销记录未返回编号");
        }
    }

    public int sumQuantity(Connection connection, long orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(SUM(quantity), 0) FROM admissions WHERE order_id = ?")) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public List<Admission> findByOrderId(long orderId) {
        String sql = "SELECT * FROM admissions WHERE order_id = ? ORDER BY admitted_at, admission_id";
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Admission> results = new ArrayList<>();
                while (resultSet.next()) results.add(map(resultSet));
                return results;
            }
        } catch (SQLException exception) {
            throw new DBException("查询核销记录失败", exception);
        }
    }

    private Admission map(ResultSet resultSet) throws SQLException {
        Admission admission = new Admission();
        admission.setAdmissionId(resultSet.getLong("admission_id"));
        admission.setOrderId(resultSet.getLong("order_id"));
        admission.setQuantity(resultSet.getInt("quantity"));
        admission.setOperatorUserId(resultSet.getLong("operator_user_id"));
        Timestamp admittedAt = resultSet.getTimestamp("admitted_at");
        admission.setAdmittedAt(admittedAt == null ? null : admittedAt.toLocalDateTime());
        admission.setNote(resultSet.getString("note"));
        return admission;
    }
}
