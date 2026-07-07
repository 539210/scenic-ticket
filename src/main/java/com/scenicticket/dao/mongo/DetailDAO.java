package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.Date;

public class DetailDAO extends MongoBaseDAO {
    public Document findByItemId(long itemId) {
        return getCollection("item_details").find(new Document("item_id", itemId)).first();
    }

    public void upsertDetail(long itemId, String description, java.util.List<String> images, Document metadata) {
        Document detail = new Document()
                .append("item_id", itemId)
                .append("description", description)
                .append("images", images)
                .append("metadata", metadata)
                .append("updated_at", new Date());
        getCollection("item_details").replaceOne(
                new Document("item_id", itemId),
                detail,
                new ReplaceOptions().upsert(true)
        );
    }
}
