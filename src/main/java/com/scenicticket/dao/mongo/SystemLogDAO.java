package com.scenicticket.dao.mongo;

import com.scenicticket.dao.MongoBaseDAO;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class SystemLogDAO extends MongoBaseDAO {
    public void insertSystemLog(Document systemLog) {
        if (!systemLog.containsKey("timestamp")) {
            systemLog.append("timestamp", new Date());
        }
        getCollection("system_logs").insertOne(systemLog);
    }

    public void insertSystemLogs(List<Document> systemLogs) {
        if (systemLogs == null || systemLogs.isEmpty()) {
            return;
        }
        List<Document> preparedLogs = new ArrayList<>();
        for (Document systemLog : systemLogs) {
            if (systemLog == null) {
                continue;
            }
            if (!systemLog.containsKey("timestamp")) {
                systemLog.append("timestamp", new Date());
            }
            if (!systemLog.containsKey("action_detail")) {
                systemLog.append("action_detail", new Document());
            }
            preparedLogs.add(systemLog);
        }
        if (!preparedLogs.isEmpty()) {
            getCollection("system_logs").insertMany(preparedLogs, new InsertManyOptions().ordered(false));
        }
    }

    public void record(long userId, String logType, String logLevel, String message, Document actionDetail) {
        Document systemLog = new Document()
                .append("user_id", userId)
                .append("log_type", logType)
                .append("log_level", logLevel)
                .append("message", message)
                .append("action_detail", actionDetail == null ? new Document() : actionDetail)
                .append("timestamp", new Date());
        insertSystemLog(systemLog);
    }

    public List<Document> findRecent(int limit) {
        return getCollection("system_logs")
                .find()
                .sort(Sorts.descending("timestamp"))
                .limit(normalizeLimit(limit))
                .into(new ArrayList<>());
    }

    public List<Document> findByCondition(Long userId, String logType, String logLevel,
                                          Date startTime, Date endTime, int limit) {
        return findByCondition(userId, logType, logLevel, startTime, endTime, null, limit);
    }

    public List<Document> findByCondition(Long userId, String logType, String logLevel,
                                          Date startTime, Date endTime, String keyword, int limit) {
        Document filter = new Document();
        if (userId != null && userId > 0) {
            filter.append("user_id", userId);
        }
        if (logType != null && !logType.isBlank()) {
            filter.append("log_type", logType.trim());
        }
        if (logLevel != null && !logLevel.isBlank()) {
            filter.append("log_level", logLevel.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String escapedKeyword = Pattern.quote(keyword.trim());
            filter.append("$or", List.of(
                    regexFilter("message", escapedKeyword),
                    regexFilter("action_detail.operation", escapedKeyword),
                    regexFilter("action_detail.ip", escapedKeyword),
                    regexFilter("action_detail.business_key", escapedKeyword),
                    regexFilter("action_detail.ticket_type_name", escapedKeyword),
                    regexFilter("action_detail.visit_date", escapedKeyword),
                    regexFilter("action_detail.payment_method", escapedKeyword),
                    regexFilter("action_detail.reason", escapedKeyword),
                    regexFilter("action_detail.tags", escapedKeyword)
            ));
        }
        return getCollection("system_logs")
                .find(withDateRange(filter, "timestamp", startTime, endTime))
                .sort(Sorts.descending("timestamp"))
                .limit(normalizeLimit(limit))
                .into(new ArrayList<>());
    }

    public List<Document> aggregateAuditSummary(Date startTime, Date endTime) {
        List<org.bson.conversions.Bson> pipeline = List.of(
                new Document("$match", withDateRange(new Document(), "timestamp", startTime, endTime)),
                new Document("$group", new Document("_id", new Document("log_type", "$log_type")
                        .append("log_level", "$log_level"))
                        .append("operation_count", new Document("$sum", 1))
                        .append("unique_users", new Document("$addToSet", "$user_id"))
                        .append("latest_timestamp", new Document("$max", "$timestamp"))),
                new Document("$project", new Document("log_type", "$_id.log_type")
                        .append("log_level", "$_id.log_level")
                        .append("operation_count", 1)
                        .append("user_count", new Document("$size", "$unique_users"))
                        .append("latest_timestamp", 1)
                        .append("_id", 0)),
                new Document("$sort", new Document("operation_count", -1).append("latest_timestamp", -1))
        );
        return getCollection("system_logs").aggregate(pipeline).into(new ArrayList<>());
    }

    public List<Document> aggregateDailyAuditTrend(Date startTime, Date endTime) {
        List<org.bson.conversions.Bson> pipeline = List.of(
                new Document("$match", withDateRange(new Document(), "timestamp", startTime, endTime)),
                new Document("$group", new Document("_id", new Document("date", new Document("$dateToString",
                        new Document("format", "%Y-%m-%d").append("date", "$timestamp")))
                        .append("log_type", "$log_type")
                        .append("log_level", "$log_level"))
                        .append("operation_count", new Document("$sum", 1))),
                new Document("$project", new Document("date", "$_id.date")
                        .append("log_type", "$_id.log_type")
                        .append("log_level", "$_id.log_level")
                        .append("operation_count", 1)
                        .append("_id", 0)),
                new Document("$sort", new Document("date", 1).append("log_type", 1).append("log_level", 1))
        );
        return getCollection("system_logs").aggregate(pipeline).into(new ArrayList<>());
    }

    public List<Document> aggregateUserOperationSummary(Date startTime, Date endTime, int limit) {
        List<org.bson.conversions.Bson> pipeline = List.of(
                new Document("$match", withDateRange(new Document(), "timestamp", startTime, endTime)),
                new Document("$group", new Document("_id", "$user_id")
                        .append("operation_count", new Document("$sum", 1))
                        .append("log_types", new Document("$addToSet", "$log_type"))
                        .append("warn_count", new Document("$sum", new Document("$cond", List.of(
                                new Document("$eq", List.of("$log_level", "WARN")), 1, 0))))
                        .append("error_count", new Document("$sum", new Document("$cond", List.of(
                                new Document("$eq", List.of("$log_level", "ERROR")), 1, 0))))
                        .append("latest_timestamp", new Document("$max", "$timestamp"))),
                new Document("$project", new Document("user_id", "$_id")
                        .append("operation_count", 1)
                        .append("log_types", 1)
                        .append("warn_count", 1)
                        .append("error_count", 1)
                        .append("latest_timestamp", 1)
                        .append("_id", 0)),
                new Document("$sort", new Document("operation_count", -1).append("latest_timestamp", -1)),
                new Document("$limit", normalizeLimit(limit))
        );
        return getCollection("system_logs").aggregate(pipeline).into(new ArrayList<>());
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

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 500);
    }

    private Document regexFilter(String field, String escapedKeyword) {
        return new Document(field, new Document("$regex", escapedKeyword).append("$options", "i"));
    }
}
