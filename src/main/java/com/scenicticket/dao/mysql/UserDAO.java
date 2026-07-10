package com.scenicticket.dao.mysql;

import com.scenicticket.dao.BaseDAO;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO extends BaseDAO {
    public long create(User user) {
        String sql = """
                INSERT INTO users (username, password_hash, email, phone, role, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPhone());
            statement.setString(5, defaultString(user.getRole(), "USER"));
            statement.setInt(6, user.getStatus() == null ? 1 : user.getStatus());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            throw new DBException("Failed to read generated user id.");
        } catch (SQLException e) {
            throw new DBException("Failed to create user.", e);
        }
    }

    public Optional<User> findById(long userId) {
        String sql = """
                SELECT user_id, username, password_hash, email, phone, role, status, created_at, updated_at
                FROM users
                WHERE user_id = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DBException("Failed to find user by id.", e);
        }
    }

    public Optional<User> findByIdForUpdate(Connection connection, long userId) throws SQLException {
        String sql = """
                SELECT user_id, username, password_hash, email, phone, role, status, created_at, updated_at
                FROM users
                WHERE user_id = ?
                FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapUser(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = """
                SELECT user_id, username, password_hash, email, phone, role, status, created_at, updated_at
                FROM users
                WHERE username = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DBException("Failed to find user by username.", e);
        }
    }

    public List<User> findAllActive(int limit, int offset) {
        String sql = """
                SELECT user_id, username, password_hash, email, phone, role, status, created_at, updated_at
                FROM users
                WHERE status = 1
                ORDER BY user_id
                LIMIT ? OFFSET ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            statement.setInt(2, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new DBException("Failed to list active users.", e);
        }
    }

    public List<User> search(String username, String email, String role, Integer status, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT user_id, username, password_hash, email, phone, role, status, created_at, updated_at
                FROM users
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();
        if (username != null && !username.isBlank()) {
            sql.append(" AND username LIKE ?");
            parameters.add("%" + username + "%");
        }
        if (email != null && !email.isBlank()) {
            sql.append(" AND email LIKE ?");
            parameters.add("%" + email + "%");
        }
        if (role != null && !role.isBlank()) {
            sql.append(" AND role = ?");
            parameters.add(role);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            parameters.add(status);
        }
        sql.append(" ORDER BY user_id LIMIT ? OFFSET ?");

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (Object parameter : parameters) {
                if (parameter instanceof Integer integer) {
                    statement.setInt(index, integer);
                } else {
                    statement.setString(index, String.valueOf(parameter));
                }
                index += 1;
            }
            statement.setInt(index, limit);
            statement.setInt(index + 1, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new DBException("Failed to search users.", e);
        }
    }

    public List<Long> lockActiveAdminIds(Connection connection) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE role = 'ADMIN' AND status = 1 ORDER BY user_id FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Long> adminIds = new ArrayList<>();
            while (resultSet.next()) {
                adminIds.add(resultSet.getLong("user_id"));
            }
            return adminIds;
        }
    }

    public boolean updateStatus(Connection connection, long userId, int status) throws SQLException {
        String sql = "UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, status);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean updateRole(Connection connection, long userId, String role) throws SQLException {
        String sql = "UPDATE users SET role = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean updateContact(long userId, String email, String phone) {
        String sql = """
                UPDATE users
                SET email = ?, phone = ?, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setString(2, phone);
            statement.setLong(3, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DBException("Failed to update user contact.", e);
        }
    }

    public boolean updateStatus(long userId, int status) {
        String sql = """
                UPDATE users
                SET status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, status);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DBException("Failed to update user status.", e);
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setUserId(resultSet.getLong("user_id"));
        user.setUsername(resultSet.getString("username"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setEmail(resultSet.getString("email"));
        user.setPhone(resultSet.getString("phone"));
        user.setRole(resultSet.getString("role"));
        user.setStatus(resultSet.getInt("status"));
        user.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        user.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return user;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
