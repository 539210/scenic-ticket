package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import org.bson.Document;

public class DetailDAO extends MongoBaseDAO {
    public Document findByItemId(long itemId) {
        return getCollection("item_details").find(new Document("item_id", itemId)).first();
    }
}
