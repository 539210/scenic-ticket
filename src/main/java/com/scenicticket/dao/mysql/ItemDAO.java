package com.scenicticket.dao.mysql;

import com.scenicticket.dao.BaseDAO;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDAO extends BaseDAO {
    public long create(Item item) {
        String sql = "INSERT INTO items (title, category_id, status) VALUES (?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, item.getTitle());
            statement.setLong(2, item.getCategoryId());
            statement.setInt(3, item.getStatus() == null ? 1 : item.getStatus());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            throw new DBException("Failed to read generated item id.");
        } catch (SQLException e) {
            throw new DBException("Failed to create item.", e);
        }
    }

    public Optional<Item> findById(long itemId) {
        String sql = """
                SELECT item_id, title, category_id, status, created_at, updated_at
                FROM items
                WHERE item_id = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapItem(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DBException("Failed to find item by id.", e);
        }
    }

    public List<Item> search(String keyword, Long categoryId, Integer status, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT item_id, title, category_id, status, created_at, updated_at
                FROM items
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND title LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        if (categoryId != null) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY item_id LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Item> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(mapItem(resultSet));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new DBException("Failed to search items.", e);
        }
    }

    public boolean update(Item item) {
        String sql = """
                UPDATE items
                SET title = ?, category_id = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE item_id = ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, item.getTitle());
            statement.setLong(2, item.getCategoryId());
            statement.setInt(3, item.getStatus() == null ? 1 : item.getStatus());
            statement.setLong(4, item.getItemId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DBException("Failed to update item.", e);
        }
    }

    public boolean updateStatus(long itemId, int status) {
        String sql = "UPDATE items SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE item_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, status);
            statement.setLong(2, itemId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DBException("Failed to update item status.", e);
        }
    }

    private void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i += 1) {
            Object value = params.get(i);
            if (value instanceof Long longValue) {
                statement.setLong(i + 1, longValue);
            } else if (value instanceof Integer integerValue) {
                statement.setInt(i + 1, integerValue);
            } else {
                statement.setString(i + 1, String.valueOf(value));
            }
        }
    }

    private Item mapItem(ResultSet resultSet) throws SQLException {
        Item item = new Item();
        item.setItemId(resultSet.getLong("item_id"));
        item.setTitle(resultSet.getString("title"));
        item.setCategoryId(resultSet.getLong("category_id"));
        item.setStatus(resultSet.getInt("status"));
        item.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        item.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return item;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
