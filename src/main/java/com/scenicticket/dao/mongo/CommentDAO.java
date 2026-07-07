package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import com.mongodb.client.model.Sorts;
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
                .find(new Document("item_id", itemId))
                .sort(Sorts.descending("created_at"))
                .limit(limit)
                .into(new ArrayList<>());
    }

    public void addComment(long userId, long itemId, String content, int rating, List<String> tags) {
        Document comment = new Document()
                .append("user_id", userId)
                .append("item_id", itemId)
                .append("content", content)
                .append("rating", rating)
                .append("tags", tags == null ? List.of() : tags)
                .append("created_at", new Date());
        insertComment(comment);
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
                new Document("$match", new Document("item_id", itemId)),
                new Document("$group", new Document("_id", "$item_id")
                        .append("comment_count", new Document("$sum", 1))
                        .append("avg_rating", new Document("$avg", "$rating"))
                        .append("max_rating", new Document("$max", "$rating"))
                        .append("min_rating", new Document("$min", "$rating"))),
                new Document("$project", new Document("item_id", "$_id")
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
                new Document("$match", new Document("item_id", itemId)),
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
                new Document("$sort", new Document("tag_count", -1)),
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
                new Document("$sort", new Document("avg_rating", -1).append("comment_count", -1)),
                new Document("$limit", limit)
        );
        return getCollection("comments").aggregate(pipeline).into(new ArrayList<>());
    }
}
