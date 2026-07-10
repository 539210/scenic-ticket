package com.scenicticket.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DBConfigTest {
    private String previousMongoDatabase;
    private String previousPoolSize;

    @org.junit.jupiter.api.BeforeEach
    void captureOverrides() {
        previousMongoDatabase = System.getProperty("scenic.ticket.mongodb.database");
        previousPoolSize = System.getProperty("scenic.ticket.mysql.pool.maximumPoolSize");
    }

    @AfterEach
    void clearOverrides() {
        restore("scenic.ticket.mongodb.database", previousMongoDatabase);
        restore("scenic.ticket.mysql.pool.maximumPoolSize", previousPoolSize);
    }

    @Test
    void systemPropertiesOverrideLocalConfigurationWithoutCopyingSecrets() {
        System.setProperty("scenic.ticket.mongodb.database", "scenic_ticket_test");
        System.setProperty("scenic.ticket.mysql.pool.maximumPoolSize", "3");

        assertEquals("scenic_ticket_test", DBConfig.get("mongodb.database"));
        assertEquals(3, DBConfig.getInt("mysql.pool.maximumPoolSize"));
    }

    private void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
