package com.scenicticket.integration;

import com.scenicticket.config.DBConfig;
import com.scenicticket.config.DatabaseTargetGuard;
import com.scenicticket.dao.mysql.ReportDAO;
import com.scenicticket.util.MySQLDBUtil;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportDatabaseObjectsIntegrationTest {
    @BeforeAll
    static void verifyTargets() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("integrationTests", "false")),
                "Set -DintegrationTests=true with explicit scenic_ticket_test overrides.");
        DatabaseTargetGuard.requireIsolatedTestTargets(DBConfig.get("mysql.url"), DBConfig.get("mongodb.database"));
    }

    @Test
    void reportDaoUsesBothViewsAndSecondStoredProcedure() throws Exception {
        ReportDAO reportDAO = new ReportDAO();
        try (Connection connection = MySQLDBUtil.getConnection()) {
            connection.setAutoCommit(false);
            long categoryId = firstCategoryId(connection);
            long itemId = insertActiveItem(connection, categoryId);
            try {
                assertTrue(reportDAO.countUserProfileViewRows(connection) >= 0);
                assertTrue(reportDAO.countItemOrderSummaryViewRows(connection) >= 0);

                reportDAO.callUpdateInactiveItems(connection, 99999);

                assertEquals(0, itemStatus(connection, itemId));
            } finally {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        }
    }

    private long firstCategoryId(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT category_id FROM categories ORDER BY category_id LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            throw new AssertionError("Test database must contain at least one category.");
        }
    }

    private long insertActiveItem(Connection connection, long categoryId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO items (title, category_id, price, discount_rate, status)
                VALUES (?, ?, ?, ?, 1)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "存储过程测试景点-" + UUID.randomUUID().toString().substring(0, 8));
            statement.setLong(2, categoryId);
            statement.setBigDecimal(3, new BigDecimal("80.00"));
            statement.setBigDecimal(4, BigDecimal.ZERO);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new AssertionError("Failed to create test item.");
        }
    }

    private int itemStatus(Connection connection, long itemId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT status FROM items WHERE item_id = ?")) {
            statement.setLong(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
            throw new AssertionError("Failed to read test item status.");
        }
    }
}
