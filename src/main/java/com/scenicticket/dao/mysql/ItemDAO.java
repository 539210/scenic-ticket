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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    public List<Item> findByIds(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> distinctIds = itemIds.stream().distinct().toList();
        String placeholders = distinctIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = """
                SELECT item_id, title, category_id, status, created_at, updated_at
                FROM items
                WHERE item_id IN (%s)
                """.formatted(placeholders);
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < distinctIds.size(); i += 1) {
                statement.setLong(i + 1, distinctIds.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Item> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(mapItem(resultSet));
                }
                return sortByInputOrder(items, distinctIds);
            }
        } catch (SQLException e) {
            throw new DBException("Failed to find items by ids.", e);
        }
    }

    public List<Item> findActiveByCategoryIds(Set<Long> categoryIds, Set<Long> excludedItemIds, int limit) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> safeExcludedIds = excludedItemIds == null ? Collections.emptySet() : new HashSet<>(excludedItemIds);
        String categoryPlaceholders = categoryIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        StringBuilder sql = new StringBuilder("""
                SELECT item_id, title, category_id, status, created_at, updated_at
                FROM items
                WHERE status = 1
                  AND category_id IN (
                """);
        sql.append(categoryPlaceholders).append(")");
        if (!safeExcludedIds.isEmpty()) {
            String excludedPlaceholders = safeExcludedIds.stream().map(id -> "?").collect(Collectors.joining(", "));
            sql.append(" AND item_id NOT IN (").append(excludedPlaceholders).append(")");
        }
        sql.append(" ORDER BY updated_at DESC, item_id DESC LIMIT ?");

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (Long categoryId : categoryIds) {
                statement.setLong(index, categoryId);
                index += 1;
            }
            for (Long excludedItemId : safeExcludedIds) {
                statement.setLong(index, excludedItemId);
                index += 1;
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Item> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(mapItem(resultSet));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new DBException("Failed to find active items by category ids.", e);
        }
    }

    public List<Item> findLatestActive(int limit) {
        String sql = """
                SELECT item_id, title, category_id, status, created_at, updated_at
                FROM items
                WHERE status = 1
                ORDER BY updated_at DESC, item_id DESC
                LIMIT ?
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Item> items = new ArrayList<>();
                while (resultSet.next()) {
                    items.add(mapItem(resultSet));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new DBException("Failed to find latest active items.", e);
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

    private List<Item> sortByInputOrder(List<Item> items, List<Long> inputIds) {
        return items.stream()
                .sorted(java.util.Comparator.comparingInt(item -> inputIds.indexOf(item.getItemId())))
                .toList();
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
