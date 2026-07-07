package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LogDAO extends MongoBaseDAO {
    public void insertActionLog(Document actionLog) {
        if (!actionLog.containsKey("created_at")) {
            actionLog.append("created_at", new Date());
        }
        getCollection("action_logs").insertOne(actionLog);
    }

    public void recordAction(long userId, long itemId, String actionType, int durationSeconds, String clientType, String ip) {
        Document actionLog = new Document()
                .append("user_id", userId)
                .append("item_id", itemId)
                .append("action_type", actionType)
                .append("duration_seconds", durationSeconds)
                .append("client_info", new Document()
                        .append("client_type", clientType)
                        .append("ip", ip))
                .append("created_at", new Date());
        insertActionLog(actionLog);
    }

    public List<Document> findRecentByUserId(long userId, int limit) {
        return getCollection("action_logs")
                .find(new Document("user_id", userId))
                .sort(Sorts.descending("created_at"))
                .limit(limit)
                .into(new ArrayList<>());
    }
}
