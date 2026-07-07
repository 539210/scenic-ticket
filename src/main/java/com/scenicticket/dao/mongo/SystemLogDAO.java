package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import org.bson.Document;

public class SystemLogDAO extends MongoBaseDAO {
    public void insertSystemLog(Document systemLog) {
        getCollection("system_logs").insertOne(systemLog);
    }
}
