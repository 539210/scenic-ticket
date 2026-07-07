package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SystemLogDAO extends MongoBaseDAO {
    public void insertSystemLog(Document systemLog) {
        if (!systemLog.containsKey("timestamp")) {
            systemLog.append("timestamp", new Date());
        }
        getCollection("system_logs").insertOne(systemLog);
    }

    public void record(long userId, String logType, String logLevel, String message, Document actionDetail) {
        Document systemLog = new Document()
                .append("user_id", userId)
                .append("log_type", logType)
                .append("log_level", logLevel)
                .append("message", message)
                .append("action_detail", actionDetail)
                .append("timestamp", new Date());
        insertSystemLog(systemLog);
    }

    public List<Document> findRecent(int limit) {
        return getCollection("system_logs")
                .find()
                .sort(Sorts.descending("timestamp"))
                .limit(limit)
                .into(new ArrayList<>());
    }
}
