package com.scenicticket.util;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.scenicticket.config.DBConfig;

public final class MongoDBUtil {
    private static final MongoClient CLIENT = MongoClients.create(DBConfig.get("mongodb.uri"));

    private MongoDBUtil() {
    }

    public static MongoDatabase getDatabase() {
        return CLIENT.getDatabase(DBConfig.get("mongodb.database"));
    }

    public static void closeClient() {
        CLIENT.close();
    }
}
