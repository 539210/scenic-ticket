package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import org.bson.Document;

public class LogDAO extends MongoBaseDAO {
    public void insertActionLog(Document actionLog) {
        getCollection("action_logs").insertOne(actionLog);
    }
}
