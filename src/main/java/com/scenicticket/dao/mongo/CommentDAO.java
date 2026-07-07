package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommentDAO extends MongoBaseDAO {
    public void insertComment(Document comment) {
        if (!comment.containsKey("created_at")) {
            comment.append("created_at", new Date());
        }
        getCollection("comments").insertOne(comment);
    }

    public List<Document> findByItemId(long itemId, int limit) {
        return getCollection("comments")
                .find(new Document("item_id", itemId))
                .sort(Sorts.descending("created_at"))
                .limit(limit)
                .into(new ArrayList<>());
    }
}
