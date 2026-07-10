package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Date;

public class DetailDAO extends MongoBaseDAO {
    public Document findByItemId(long itemId) {
        Document numeric = getCollection("item_details").find(Filters.eq("item_id", itemId)).first();
        return numeric != null
                ? numeric
                : getCollection("item_details").find(Filters.eq("item_id", String.valueOf(itemId))).first();
    }

    public void upsertDetail(long itemId, String description, java.util.List<String> images, Document metadata) {
        Document existing = findByItemId(itemId);
        Document detail = new Document()
                .append("item_id", itemId)
                .append("description", description)
                .append("images", images)
                .append("metadata", metadata)
                .append("updated_at", new Date());
        Bson filter = existing != null && existing.get("_id") != null
                ? Filters.eq("_id", existing.get("_id"))
                : Filters.eq("item_id", itemId);
        getCollection("item_details").replaceOne(
                filter,
                detail,
                new ReplaceOptions().upsert(true)
        );
    }
}
