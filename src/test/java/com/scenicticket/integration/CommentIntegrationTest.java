package com.scenicticket.integration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.scenicticket.config.DBConfig;
import com.scenicticket.config.DatabaseTargetGuard;
import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.service.CommentService;
import com.scenicticket.service.OrderLifecycleService;
import com.scenicticket.service.TicketInventoryService;
import com.scenicticket.util.MongoDBUtil;
import com.scenicticket.util.MySQLDBUtil;
import org.bson.Document;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentIntegrationTest {
    private static final long USER_ID = 2L;
    private static final long ITEM_ID = 1L;
    private static final String LEGACY_USER_ID = "987654321";

    @BeforeAll
    static void verifyTargets() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("integrationTests", "false")),
                "Set -DintegrationTests=true with explicit scenic_ticket_test overrides.");
        DatabaseTargetGuard.requireIsolatedTestTargets(DBConfig.get("mysql.url"), DBConfig.get("mongodb.database"));
    }

    @Test
    void paidOrderAllowsOneUtf8CommentThatCanBeUpdatedAndAggregated() throws Exception {
        MongoCollection<Document> comments = MongoDBUtil.getDatabase().getCollection("comments");
        comments.createIndex(Indexes.compoundIndex(Indexes.ascending("user_id"), Indexes.ascending("item_id")),
                new IndexOptions().unique(true).name("uq_comments_user_item"));
        comments.deleteMany(compatibleCommentFilter());
        comments.deleteMany(legacyCommentFilter());
        comments.insertOne(new Document("user_id", LEGACY_USER_ID)
                .append("item_id", String.valueOf(ITEM_ID))
                .append("content", "历史字符串ID评论")
                .append("rating", 3)
                .append("tags", List.of("历史兼容"))
                .append("created_at", new Date())
                .append("updated_at", new Date()));
        long baselineItemComments = comments.countDocuments(
                Filters.in("item_id", ITEM_ID, String.valueOf(ITEM_ID)));

        TicketInventoryService inventoryService = new TicketInventoryService();
        OrderLifecycleService lifecycleService = new OrderLifecycleService();
        CommentService commentService = new CommentService();
        LocalDate visitDate = LocalDate.now().plusDays(5);
        long ticketTypeId = inventoryService.createTicketType(1L, ITEM_ID,
                "评论资格票" + UUID.randomUUID().toString().substring(0, 8),
                new BigDecimal("30.00"), BigDecimal.ZERO);
        long orderId = 0L;
        try {
            inventoryService.setTotalStock(1L, ticketTypeId, visitDate, 2);
            orderId = lifecycleService.createPendingOrder(USER_ID, ticketTypeId, visitDate, 1,
                    "微信", "127.0.0.1").orderId();
            lifecycleService.pay(USER_ID, orderId, "127.0.0.1");

            var created = commentService.submit(USER_ID, ITEM_ID, "首次中文评论", 5,
                    List.of("景色好", "交通方便"), "127.0.0.1");
            Document first = new CommentDAO().findByUserAndItem(USER_ID, ITEM_ID);
            assertFalse(created.updated());
            assertNotNull(first);
            Date createdAt = first.getDate("created_at");
            assertNotNull(createdAt);

            var updated = commentService.submit(USER_ID, ITEM_ID, "更新后的中文评论", 4,
                    List.of("适合家庭"), "127.0.0.1");
            Document saved = new CommentDAO().findByUserAndItem(USER_ID, ITEM_ID);
            assertTrue(updated.updated());
            assertEquals(1L, comments.countDocuments(compatibleCommentFilter()));
            assertEquals(createdAt, saved.getDate("created_at"));
            assertTrue(!saved.getDate("updated_at").before(createdAt));
            assertTrue(saved.get("user_id") instanceof Number);
            assertTrue(saved.get("item_id") instanceof Number);
            assertEquals("更新后的中文评论", saved.getString("content"));
            assertEquals(List.of("适合家庭"), saved.getList("tags", String.class));

            var list = commentService.listForItem(USER_ID, ITEM_ID, 100);
            assertEquals(baselineItemComments + 1, list.comments().size());
            var ownView = list.comments().stream()
                    .filter(view -> view.userId() == USER_ID)
                    .findFirst().orElseThrow();
            assertEquals("u***", ownView.displayUsername());
            assertEquals("更新后的中文评论", ownView.content());
            assertEquals(baselineItemComments + 1,
                    ((Number) list.ratingSummary().get("comment_count")).longValue());
            assertTrue(new CommentDAO().aggregateRatingDistribution(ITEM_ID).stream()
                    .anyMatch(row -> ((Number) row.get("_id")).intValue() == 4
                            && ((Number) row.get("count")).intValue() >= 1));
        } finally {
            comments.deleteMany(compatibleCommentFilter());
            comments.deleteMany(legacyCommentFilter());
            MongoDBUtil.getDatabase().getCollection("action_logs").deleteMany(Filters.and(
                    Filters.in("user_id", USER_ID, String.valueOf(USER_ID)),
                    Filters.in("item_id", ITEM_ID, String.valueOf(ITEM_ID)),
                    Filters.eq("action_type", "COMMENT")));
            cleanupMySql(orderId, ticketTypeId, visitDate);
        }
    }

    private static org.bson.conversions.Bson compatibleCommentFilter() {
        return Filters.and(
                Filters.in("user_id", USER_ID, String.valueOf(USER_ID)),
                Filters.in("item_id", ITEM_ID, String.valueOf(ITEM_ID)));
    }

    private static org.bson.conversions.Bson legacyCommentFilter() {
        return Filters.and(Filters.eq("user_id", LEGACY_USER_ID),
                Filters.eq("item_id", String.valueOf(ITEM_ID)));
    }

    private static void cleanupMySql(long orderId, long ticketTypeId, LocalDate visitDate) throws Exception {
        try (Connection connection = MySQLDBUtil.getConnection()) {
            if (orderId > 0) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM orders WHERE order_id = ?")) {
                    statement.setLong(1, orderId);
                    statement.executeUpdate();
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM ticket_inventory WHERE ticket_type_id = ? AND visit_date = ?")) {
                statement.setLong(1, ticketTypeId);
                statement.setDate(2, java.sql.Date.valueOf(visitDate));
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM ticket_types WHERE ticket_type_id = ?")) {
                statement.setLong(1, ticketTypeId);
                statement.executeUpdate();
            }
        }
    }
}
