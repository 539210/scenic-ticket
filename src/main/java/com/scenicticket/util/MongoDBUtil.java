package com.scenicticket.util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.scenicticket.config.DBConfig;

public final class MongoDBUtil {
    private static volatile MongoClient client;

    private MongoDBUtil() {
    }

    public static MongoDatabase getDatabase() {
        return getClient().getDatabase(DBConfig.get("mongodb.database"));
    }

    public static synchronized void closeClient() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    private static MongoClient getClient() {
        MongoClient current = client;
        if (current != null) {
            return current;
        }
        synchronized (MongoDBUtil.class) {
            if (client == null) {
                client = MongoClients.create(DBConfig.get("mongodb.uri"));
            }
            return client;
        }
    }
}
