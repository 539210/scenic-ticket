package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.conversions.Bson;
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

    public List<Document> findByItemId(long itemId, String actionType, int limit) {
        Document filter = new Document("item_id", itemId);
        if (actionType != null && !actionType.isBlank()) {
            filter.append("action_type", actionType);
        }
        return getCollection("action_logs")
                .find(filter)
                .sort(Sorts.descending("created_at"))
                .limit(limit)
                .into(new ArrayList<>());
    }

    public List<Document> aggregateUserBehavior(long userId, Date startTime, Date endTime) {
        List<Bson> pipeline = List.of(
                new Document("$match", withDateRange(new Document("user_id", userId), "created_at", startTime, endTime)),
                new Document("$group", new Document("_id", "$action_type")
                        .append("action_count", new Document("$sum", 1))
                        .append("total_duration", new Document("$sum", "$duration_seconds"))
                        .append("avg_duration", new Document("$avg", "$duration_seconds"))
                        .append("latest_action_time", new Document("$max", "$created_at"))),
                new Document("$sort", new Document("action_count", -1))
        );
        return getCollection("action_logs").aggregate(pipeline).into(new ArrayList<>());
    }

    public Document aggregateUserReport(long userId, Date startTime, Date endTime) {
        List<Bson> pipeline = List.of(
                new Document("$match", withDateRange(new Document("user_id", userId), "created_at", startTime, endTime)),
                new Document("$group", new Document("_id", "$user_id")
                        .append("action_count", new Document("$sum", 1))
                        .append("visited_items", new Document("$addToSet", "$item_id"))
                        .append("total_duration", new Document("$sum", "$duration_seconds"))
                        .append("avg_duration", new Document("$avg", "$duration_seconds"))
                        .append("view_count", new Document("$sum", new Document("$cond", List.of(
                                new Document("$eq", List.of("$action_type", "VIEW")), 1, 0))))
                        .append("search_count", new Document("$sum", new Document("$cond", List.of(
                                new Document("$eq", List.of("$action_type", "SEARCH")), 1, 0))))
                        .append("comment_count", new Document("$sum", new Document("$cond", List.of(
                                new Document("$eq", List.of("$action_type", "COMMENT")), 1, 0))))
                        .append("order_count", new Document("$sum", new Document("$cond", List.of(
                                new Document("$eq", List.of("$action_type", "ORDER")), 1, 0))))
                        .append("first_action_time", new Document("$min", "$created_at"))
                        .append("latest_action_time", new Document("$max", "$created_at"))),
                new Document("$project", new Document("user_id", "$_id")
                        .append("action_count", 1)
                        .append("visited_item_count", new Document("$size", "$visited_items"))
                        .append("total_duration", 1)
                        .append("avg_duration", 1)
                        .append("view_count", 1)
                        .append("search_count", 1)
                        .append("comment_count", 1)
                        .append("order_count", 1)
                        .append("first_action_time", 1)
                        .append("latest_action_time", 1)
                        .append("_id", 0))
        );
        Document report = getCollection("action_logs").aggregate(pipeline).first();
        return report == null ? new Document("user_id", userId).append("action_count", 0) : report;
    }

    public List<Document> aggregateHotItems(Date startTime, Date endTime, int limit) {
        List<Bson> pipeline = List.of(
                new Document("$match", withDateRange(new Document(), "created_at", startTime, endTime)),
                new Document("$group", new Document("_id", "$item_id")
                        .append("total_actions", new Document("$sum", 1))
                        .append("view_count", new Document("$sum", new Document("$cond", List.of(
                                new Document("$eq", List.of("$action_type", "VIEW")), 1, 0))))
                        .append("order_count", new Document("$sum", new Document("$cond", List.of(
                                new Document("$eq", List.of("$action_type", "ORDER")), 1, 0))))
                        .append("avg_duration", new Document("$avg", "$duration_seconds"))),
                new Document("$sort", new Document("total_actions", -1).append("view_count", -1)),
                new Document("$limit", limit)
        );
        return getCollection("action_logs").aggregate(pipeline).into(new ArrayList<>());
    }

    public List<Document> aggregateUserItemScores(long userId, int limit) {
        List<Bson> pipeline = List.of(
                new Document("$match", new Document("user_id", userId).append("item_id", new Document("$gt", 0))),
                new Document("$group", new Document("_id", "$item_id")
                        .append("view_count", new Document("$sum", new Document("$cond", List.of(
                                new Document("$eq", List.of("$action_type", "VIEW")), 1, 0))))
                        .append("comment_count", new Document("$sum", new Document("$cond", List.of(
                                new Document("$eq", List.of("$action_type", "COMMENT")), 1, 0))))
                        .append("order_count", new Document("$sum", new Document("$cond", List.of(
                                new Document("$eq", List.of("$action_type", "ORDER")), 1, 0))))
                        .append("latest_action_time", new Document("$max", "$created_at"))),
                new Document("$addFields", new Document("interest_score", new Document("$add", List.of(
                        "$view_count",
                        new Document("$multiply", List.of("$comment_count", 3)),
                        new Document("$multiply", List.of("$order_count", 5))
                )))),
                new Document("$sort", new Document("interest_score", -1).append("latest_action_time", -1)),
                new Document("$limit", limit)
        );
        return getCollection("action_logs").aggregate(pipeline).into(new ArrayList<>());
    }

    public List<Document> aggregateActionTypeSummary(Date startTime, Date endTime) {
        List<Bson> pipeline = List.of(
                new Document("$match", withDateRange(new Document(), "created_at", startTime, endTime)),
                new Document("$group", new Document("_id", "$action_type")
                        .append("action_count", new Document("$sum", 1))
                        .append("unique_users", new Document("$addToSet", "$user_id"))
                        .append("latest_action_time", new Document("$max", "$created_at"))),
                new Document("$project", new Document("action_type", "$_id")
                        .append("action_count", 1)
                        .append("user_count", new Document("$size", "$unique_users"))
                        .append("latest_action_time", 1)
                        .append("_id", 0)),
                new Document("$sort", new Document("action_count", -1))
        );
        return getCollection("action_logs").aggregate(pipeline).into(new ArrayList<>());
    }

    public List<Document> aggregateDailyTrend(Date startTime, Date endTime) {
        List<Bson> pipeline = List.of(
                new Document("$match", withDateRange(new Document(), "created_at", startTime, endTime)),
                new Document("$group", new Document("_id", new Document("date", new Document("$dateToString",
                        new Document("format", "%Y-%m-%d").append("date", "$created_at")))
                        .append("action_type", "$action_type"))
                        .append("action_count", new Document("$sum", 1))),
                new Document("$sort", new Document("_id.date", 1).append("_id.action_type", 1))
        );
        return getCollection("action_logs").aggregate(pipeline).into(new ArrayList<>());
    }

    public long countByUserId(long userId) {
        return getCollection("action_logs").countDocuments(Filters.eq("user_id", userId));
    }

    private Document withDateRange(Document filter, String field, Date startTime, Date endTime) {
        Document range = new Document();
        if (startTime != null) {
            range.append("$gte", startTime);
        }
        if (endTime != null) {
            range.append("$lte", endTime);
        }
        if (!range.isEmpty()) {
            filter.append(field, range);
        }
        return filter;
    }
}
