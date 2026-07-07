package com.scenicticket.dao.mysql;

import com.scenicticket.dao.BaseDAO;
import com.scenicticket.exception.DBException;
import com.scenicticket.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDAO extends BaseDAO {
    public long create(Category category) {
        String sql = "INSERT INTO categories (name, parent_id) VALUES (?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, category.getName());
            if (category.getParentId() == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, category.getParentId());
            }
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
            throw new DBException("Failed to read generated category id.");
        } catch (SQLException e) {
            throw new DBException("Failed to create category.", e);
        }
    }

    public Optional<Category> findById(long categoryId) {
        String sql = "SELECT category_id, name, parent_id FROM categories WHERE category_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, categoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapCategory(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DBException("Failed to find category by id.", e);
        }
    }

    public List<Category> findAll() {
        String sql = "SELECT category_id, name, parent_id FROM categories ORDER BY parent_id, category_id";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Category> categories = new ArrayList<>();
            while (resultSet.next()) {
                categories.add(mapCategory(resultSet));
            }
            return categories;
        } catch (SQLException e) {
            throw new DBException("Failed to list categories.", e);
        }
    }

    public List<Category> findChildren(Long parentId) {
        String sql = parentId == null
                ? "SELECT category_id, name, parent_id FROM categories WHERE parent_id IS NULL ORDER BY category_id"
                : "SELECT category_id, name, parent_id FROM categories WHERE parent_id = ? ORDER BY category_id";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parentId != null) {
                statement.setLong(1, parentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Category> categories = new ArrayList<>();
                while (resultSet.next()) {
                    categories.add(mapCategory(resultSet));
                }
                return categories;
            }
        } catch (SQLException e) {
            throw new DBException("Failed to list child categories.", e);
        }
    }

    public boolean update(Category category) {
        String sql = "UPDATE categories SET name = ?, parent_id = ? WHERE category_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getName());
            if (category.getParentId() == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
            } else {
                statement.setLong(2, category.getParentId());
            }
            statement.setLong(3, category.getCategoryId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DBException("Failed to update category.", e);
        }
    }

    private Category mapCategory(ResultSet resultSet) throws SQLException {
        Category category = new Category();
        category.setCategoryId(resultSet.getLong("category_id"));
        category.setName(resultSet.getString("name"));
        long parentId = resultSet.getLong("parent_id");
        category.setParentId(resultSet.wasNull() ? null : parentId);
        return category;
    }
}
