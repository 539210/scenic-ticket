package com.scenicticket.config;

import com.scenicticket.exception.DBException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseTargetGuardTest {
    @Test
    void extractsDatabaseNameFromJdbcUrl() {
        assertEquals("scenic_ticket_test", DatabaseTargetGuard.mysqlDatabaseName(
                "jdbc:mysql://localhost:3306/scenic_ticket_test?useUnicode=true&serverTimezone=Asia/Shanghai"));
    }

    @Test
    void acceptsOnlyBothIsolatedTestTargets() {
        assertDoesNotThrow(() -> DatabaseTargetGuard.requireIsolatedTestTargets(
                "jdbc:mysql://localhost:3306/scenic_ticket_test?useSSL=false", "scenic_ticket_test"));
        assertThrows(DBException.class, () -> DatabaseTargetGuard.requireIsolatedTestTargets(
                "jdbc:mysql://localhost:3306/scenic_ticket?useSSL=false", "scenic_ticket_test"));
        assertThrows(DBException.class, () -> DatabaseTargetGuard.requireIsolatedTestTargets(
                "jdbc:mysql://localhost:3306/scenic_ticket_test?useSSL=false", "scenic_ticket"));
    }
}
