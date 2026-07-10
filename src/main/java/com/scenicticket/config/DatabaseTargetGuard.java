package com.scenicticket.config;

import com.scenicticket.exception.DBException;

import java.net.URI;
import java.net.URISyntaxException;

public final class DatabaseTargetGuard {
    public static final String TEST_DATABASE = "scenic_ticket_test";

    private DatabaseTargetGuard() {
    }

    public static void requireIsolatedTestTargets(String mysqlJdbcUrl, String mongoDatabase) {
        String mysqlDatabase = mysqlDatabaseName(mysqlJdbcUrl);
        if (!TEST_DATABASE.equals(mysqlDatabase)) {
            throw new DBException("Integration tests must use MySQL database " + TEST_DATABASE
                    + ", but configured database is " + display(mysqlDatabase));
        }
        if (!TEST_DATABASE.equals(mongoDatabase)) {
            throw new DBException("Integration tests must use MongoDB database " + TEST_DATABASE
                    + ", but configured database is " + display(mongoDatabase));
        }
    }

    public static String mysqlDatabaseName(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:mysql://")) {
            throw new DBException("Invalid MySQL JDBC URL");
        }
        try {
            URI uri = new URI(jdbcUrl.substring("jdbc:".length()));
            String path = uri.getPath();
            if (path == null || path.length() <= 1) {
                throw new DBException("MySQL JDBC URL does not contain a database name");
            }
            return path.substring(1);
        } catch (URISyntaxException e) {
            throw new DBException("Invalid MySQL JDBC URL", e);
        }
    }

    private static String display(String database) {
        return database == null || database.isBlank() ? "<missing>" : database;
    }
}
