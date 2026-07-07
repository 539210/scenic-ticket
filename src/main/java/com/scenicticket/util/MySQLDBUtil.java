package com.scenicticket.util;

import com.scenicticket.config.DBConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class MySQLDBUtil {
    private static final HikariDataSource DATA_SOURCE = createDataSource();

    private MySQLDBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    public static void closeDataSource() {
        DATA_SOURCE.close();
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DBConfig.get("mysql.url"));
        config.setUsername(DBConfig.get("mysql.username"));
        config.setPassword(DBConfig.get("mysql.password"));
        config.setMaximumPoolSize(DBConfig.getInt("mysql.pool.maximumPoolSize"));
        config.setMinimumIdle(DBConfig.getInt("mysql.pool.minimumIdle"));
        config.setConnectionTimeout(DBConfig.getInt("mysql.pool.connectionTimeoutMs"));
        config.setIdleTimeout(DBConfig.getInt("mysql.pool.idleTimeoutMs", 600000));
        config.setMaxLifetime(DBConfig.getInt("mysql.pool.maxLifetimeMs", 1800000));
        config.setValidationTimeout(DBConfig.getInt("mysql.pool.validationTimeoutMs", 5000));
        config.setLeakDetectionThreshold(DBConfig.getInt("mysql.pool.leakDetectionThresholdMs", 0));
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.setPoolName("scenic-ticket-mysql-pool");
        return new HikariDataSource(config);
    }
}
