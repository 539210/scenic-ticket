package com.scenicticket.config;

import com.scenicticket.exception.DBException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DBConfig {
    private static final String CONFIG_FILE = "db.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream inputStream = DBConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new DBException("Database configuration file not found: " + CONFIG_FILE);
            }
            PROPERTIES.load(inputStream);
        } catch (IOException e) {
            throw new DBException("Failed to load database configuration.", e);
        }
    }

    private DBConfig() {
    }

    public static String get(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new DBException("Missing required database configuration: " + key);
        }
        return value.trim();
    }

    public static int getInt(String key) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            throw new DBException("Invalid integer database configuration: " + key, e);
        }
    }

    public static int getInt(String key, int defaultValue) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new DBException("Invalid integer database configuration: " + key, e);
        }
    }
}
