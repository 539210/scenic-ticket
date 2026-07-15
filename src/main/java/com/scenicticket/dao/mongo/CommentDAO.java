package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.conversions.Bson;
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
                .find(Filters.in("item_id", itemId, String.valueOf(itemId)))
                .sort(Sorts.descending("created_at"))
                .limit(limit)
                .into(new ArrayList<>());
    }

    public void addComment(long userId, long itemId, String content, int rating, List<String> tags) {
        upsertComment(userId, itemId, content, rating, tags);
    }

    public Document findByUserAndItem(long userId, long itemId) {
        return getCollection("comments").find(Filters.and(
                Filters.in("user_id", userId, String.valueOf(userId)),
                Filters.in("item_id", itemId, String.valueOf(itemId)))).first();
    }

    public Document upsertComment(long userId, long itemId, String content, int rating, List<String> tags) {
        MongoCollection<Document> comments = getCollection("comments");
        Document existing = findByUserAndItem(userId, itemId);
        Bson filter = existing != null && existing.get("_id") != null
                ? Filters.eq("_id", existing.get("_id"))
                : Filters.and(Filters.eq("user_id", userId), Filters.eq("item_id", itemId));
        Date now = new Date();
        Bson update = Updates.combine(
                Updates.set("user_id", userId),
                Updates.set("item_id", itemId),
                Updates.set("content", content),
                Updates.set("rating", rating),
                Updates.set("tags", tags == null ? List.of() : tags),
                Updates.set("updated_at", now),
                Updates.setOnInsert("created_at", now));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .upsert(true).returnDocument(ReturnDocument.AFTER);
        try {
            return comments.findOneAndUpdate(filter, update, options);
        } catch (MongoWriteException exception) {
            if (exception.getError().getCode() != 11000) {
                throw exception;
            }
            // A concurrent first submission may win the unique (user_id, item_id) insert.
            // Retry as an update so both callers converge on the same comment document.
            return comments.findOneAndUpdate(
                    Filters.and(Filters.eq("user_id", userId), Filters.eq("item_id", itemId)),
                    update,
                    new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        }
    }

    public List<Document> findByUserId(long userId, int limit) {
        return getCollection("comments")
                .find(new Document("user_id", userId))
                .sort(Sorts.descending("created_at"))
                .limit(limit)
                .into(new ArrayList<>());
    }

    public Document aggregateRatingByItem(long itemId) {
        List<Bson> pipeline = List.of(
                new Document("$match", new Document("item_id", new Document("$in", List.of(itemId, String.valueOf(itemId))))),
                new Document("$group", new Document("_id", null)
                        .append("comment_count", new Document("$sum", 1))
                        .append("avg_rating", new Document("$avg", "$rating"))
                        .append("max_rating", new Document("$max", "$rating"))
                        .append("min_rating", new Document("$min", "$rating"))),
                new Document("$project", new Document("item_id", new Document("$literal", itemId))
                        .append("comment_count", 1)
                        .append("avg_rating", 1)
                        .append("max_rating", 1)
                        .append("min_rating", 1)
                        .append("_id", 0))
        );
        return getCollection("comments").aggregate(pipeline).first();
    }

    public List<Document> aggregateRatingDistribution(long itemId) {
        List<Bson> pipeline = List.of(
                new Document("$match", new Document("item_id", new Document("$in", List.of(itemId, String.valueOf(itemId))))),
                new Document("$group", new Document("_id", "$rating")
                        .append("count", new Document("$sum", 1))),
                new Document("$sort", new Document("_id", 1))
        );
        return getCollection("comments").aggregate(pipeline).into(new ArrayList<>());
    }

    public List<Document> aggregateHotTags(int limit) {
        List<Bson> pipeline = List.of(
                new Document("$unwind", "$tags"),
                new Document("$group", new Document("_id", "$tags")
                        .append("tag_count", new Document("$sum", 1))),
                new Document("$sort", new Document("tag_count", -1).append("_id", 1)),
                new Document("$limit", limit)
        );
        return getCollection("comments").aggregate(pipeline).into(new ArrayList<>());
    }

    public List<Document> aggregateTopRatedItems(int limit) {
        List<Bson> pipeline = List.of(
                new Document("$group", new Document("_id", "$item_id")
                        .append("comment_count", new Document("$sum", 1))
                        .append("avg_rating", new Document("$avg", "$rating"))),
                new Document("$match", new Document("comment_count", new Document("$gte", 1))),
                new Document("$sort", new Document("avg_rating", -1)
                        .append("comment_count", -1).append("_id", 1)),
                new Document("$limit", limit)
        );
        return getCollection("comments").aggregate(pipeline).into(new ArrayList<>());
    }
}
