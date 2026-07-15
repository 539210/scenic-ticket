package com.scenicticket.integration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Filters;
import com.scenicticket.config.DBConfig;
import com.scenicticket.config.DatabaseTargetGuard;
import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.DetailDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.util.MongoDBUtil;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MongoSchemaIntegrationTest {
    private static MongoDatabase database;
    private static String previousMysqlUrl;
    private static String previousMongoDatabase;

    @BeforeAll
    static void initializeIsolatedDatabase() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("integrationTests", "false")),
                "Set -DintegrationTests=true to run real database integration tests.");

        previousMysqlUrl = System.getProperty("scenic.ticket.mysql.url");
        previousMongoDatabase = System.getProperty("scenic.ticket.mongodb.database");
        String configuredMysqlUrl = DBConfig.get("mysql.url");
        String testMysqlUrl = configuredMysqlUrl.replaceFirst("/[^/?]+(?=\\?|$)", "/scenic_ticket_test");
        System.setProperty("scenic.ticket.mysql.url", testMysqlUrl);
        System.setProperty("scenic.ticket.mongodb.database", DatabaseTargetGuard.TEST_DATABASE);
        DatabaseTargetGuard.requireIsolatedTestTargets(
                DBConfig.get("mysql.url"), DBConfig.get("mongodb.database"));

        database = MongoDBUtil.getDatabase();
        database.drop();
        createCollectionsAndIndexes(database);
        insertSeedData(database);
    }

    @AfterAll
    static void closeClient() {
        if (database != null) {
            MongoDBUtil.closeClient();
        }
        restore("scenic.ticket.mysql.url", previousMysqlUrl);
        restore("scenic.ticket.mongodb.database", previousMongoDatabase);
    }

    @Test
    void createsFourRequiredCollectionsAndIndexes() {
        Set<String> collectionNames = database.listCollectionNames().into(new ArrayList<>()).stream()
                .collect(Collectors.toSet());
        assertTrue(collectionNames.containsAll(Set.of("action_logs", "comments", "item_details", "system_logs")));

        assertTrue(indexNames(database.getCollection("item_details")).contains("uq_item_details_item_id"));
        assertTrue(indexNames(database.getCollection("comments")).contains("uq_comments_user_item"));
        assertTrue(indexNames(database.getCollection("system_logs")).contains("idx_system_logs_audit"));
    }

    @Test
    void preservesChineseTextAndUsesNumericIds() {
        Document detail = new DetailDAO().findByItemId(1L);
        List<Document> comments = new CommentDAO().findByItemId(1L, 20);

        assertNotNull(detail);
        assertEquals("景点 1 的详细介绍，包含开放时间、游览路线、购票提示和入园须知。",
                detail.getString("description"));
        assertFalse(comments.isEmpty());
        assertTrue(comments.get(0).get("user_id") instanceof Number);
        assertTrue(comments.get(0).get("item_id") instanceof Number);
        assertTrue(comments.get(0).getString("content").contains("景区体验良好"));
    }

    @Test
    void readsAndNormalizesLegacyStringItemIdDetails() {
        long itemId = 900001L;
        database.getCollection("item_details").insertOne(new Document("item_id", String.valueOf(itemId))
                .append("description", "旧字符串 ID 简介")
                .append("images", List.of("legacy.jpg"))
                .append("metadata", new Document("source", "legacy"))
                .append("updated_at", new Date()));

        DetailDAO detailDAO = new DetailDAO();
        assertEquals("旧字符串 ID 简介", detailDAO.findByItemId(itemId).getString("description"));

        detailDAO.upsertDetail(itemId, "已迁移的中文简介", List.of("current.jpg"),
                new Document("source", "migrated"));

        List<Document> compatibleRows = database.getCollection("item_details")
                .find(Filters.in("item_id", itemId, String.valueOf(itemId))).into(new ArrayList<>());
        assertEquals(1, compatibleRows.size());
        assertTrue(compatibleRows.get(0).get("item_id") instanceof Number);
        assertEquals("已迁移的中文简介", compatibleRows.get(0).getString("description"));
    }

    @Test
    void requiredAggregationsReturnRealResults() {
        LogDAO logDAO = new LogDAO();
        CommentDAO commentDAO = new CommentDAO();
        SystemLogDAO systemLogDAO = new SystemLogDAO();

        assertFalse(logDAO.aggregateHotItems(null, null, 10).isEmpty());
        assertFalse(logDAO.aggregateUserBehavior(1L, null, null).isEmpty());
        assertFalse(commentDAO.aggregateTopRatedItems(10).isEmpty());
        assertFalse(systemLogDAO.aggregateAuditSummary(null, null).isEmpty());
        assertFalse(systemLogDAO.findByCondition(1L, "LOGIN", "INFO", null, null, 20).isEmpty());
    }

    @Test
    void auditQueryCombinesUserTypeLevelDateKeywordAndLimit() {
        SystemLogDAO systemLogDAO = new SystemLogDAO();
        Date now = new Date();
        database.getCollection("system_logs").insertOne(new Document("user_id", 88001L)
                .append("log_type", "PAYMENT")
                .append("log_level", "WARN")
                .append("message", "支付确认审计关键词")
                .append("action_detail", new Document("operation", "支付确认").append("ip", "127.0.0.1"))
                .append("timestamp", now));

        List<Document> results = systemLogDAO.findByCondition(88001L, "PAYMENT", "WARN",
                new Date(now.getTime() - 1_000L), new Date(now.getTime() + 1_000L), "支付确认", 1);

        assertEquals(1, results.size());
        assertEquals("支付确认审计关键词", results.get(0).getString("message"));
    }

    private static void createCollectionsAndIndexes(MongoDatabase target) {
        for (String name : List.of("action_logs", "comments", "item_details", "system_logs")) {
            target.createCollection(name);
        }

        MongoCollection<Document> actions = target.getCollection("action_logs");
        actions.createIndex(Indexes.compoundIndex(Indexes.ascending("user_id"), Indexes.descending("created_at")),
                new IndexOptions().name("idx_action_logs_user_time"));
        actions.createIndex(Indexes.compoundIndex(Indexes.ascending("item_id", "action_type")),
                new IndexOptions().name("idx_action_logs_item_type"));

        MongoCollection<Document> comments = target.getCollection("comments");
        comments.createIndex(Indexes.compoundIndex(Indexes.ascending("item_id"), Indexes.descending("created_at")),
                new IndexOptions().name("idx_comments_item_time"));
        comments.createIndex(Indexes.compoundIndex(Indexes.ascending("user_id", "item_id")),
                new IndexOptions().name("uq_comments_user_item").unique(true));

        target.getCollection("item_details").createIndex(Indexes.ascending("item_id"),
                new IndexOptions().name("uq_item_details_item_id").unique(true));

        MongoCollection<Document> systemLogs = target.getCollection("system_logs");
        systemLogs.createIndex(Indexes.compoundIndex(Indexes.ascending("user_id", "log_type", "log_level"),
                        Indexes.descending("timestamp")),
                new IndexOptions().name("idx_system_logs_audit"));
        systemLogs.createIndex(Indexes.text("message"), new IndexOptions().name("idx_system_logs_message_text"));
    }

    private static void insertSeedData(MongoDatabase target) {
        List<Document> details = new ArrayList<>();
        List<Document> comments = new ArrayList<>();
        List<Document> actions = new ArrayList<>();
        List<Document> systemLogs = new ArrayList<>();
        Date now = new Date();

        for (long itemId = 1; itemId <= 20; itemId += 1) {
            details.add(new Document("item_id", itemId)
                    .append("description", "景点 " + itemId + " 的详细介绍，包含开放时间、游览路线、购票提示和入园须知。")
                    .append("images", List.of("https://example.com/scenic-" + itemId + "-1.jpg"))
                    .append("metadata", new Document("language", "zh-CN").append("open_time", "08:00-18:00"))
                    .append("updated_at", now));
            comments.add(new Document("user_id", itemId)
                    .append("item_id", itemId)
                    .append("content", "景区体验良好，购票流程顺畅。")
                    .append("rating", (int) (itemId % 5) + 1)
                    .append("tags", List.of("环境好", "购票方便"))
                    .append("created_at", now)
                    .append("updated_at", now));
        }

        for (int index = 0; index < 100; index += 1) {
            long userId = index % 20 + 1L;
            long itemId = index % 20 + 1L;
            String actionType = switch (index % 4) {
                case 0 -> "VIEW";
                case 1 -> "SEARCH";
                case 2 -> "ORDER";
                default -> "COMMENT";
            };
            actions.add(new Document("user_id", userId)
                    .append("item_id", itemId)
                    .append("action_type", actionType)
                    .append("duration_seconds", 30 + index)
                    .append("client_info", new Document("client_type", "INTEGRATION").append("ip", "127.0.0.1"))
                    .append("created_at", now));

            String logType = index % 2 == 0 ? "LOGIN" : "REPORT_VIEW";
            systemLogs.add(new Document("user_id", userId)
                    .append("log_type", logType)
                    .append("log_level", "INFO")
                    .append("message", "集成测试系统日志 " + index)
                    .append("action_detail", new Document("operation", logType).append("ip", "127.0.0.1"))
                    .append("timestamp", now));
        }

        target.getCollection("item_details").insertMany(details);
        target.getCollection("comments").insertMany(comments);
        target.getCollection("action_logs").insertMany(actions);
        target.getCollection("system_logs").insertMany(systemLogs);
    }

    private static Set<String> indexNames(MongoCollection<Document> collection) {
        return collection.listIndexes().into(new ArrayList<>()).stream()
                .map(index -> index.getString("name"))
                .collect(Collectors.toSet());
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
