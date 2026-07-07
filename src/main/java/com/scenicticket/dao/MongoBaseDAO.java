package com.scenicticket.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.scenicticket.util.MongoDBUtil;
import org.bson.Document;

public abstract class MongoBaseDAO {
    protected MongoDatabase getDatabase() {
        return MongoDBUtil.getDatabase();
    }

    protected MongoCollection<Document> getCollection(String collectionName) {
        return getDatabase().getCollection(collectionName);
    }
}
