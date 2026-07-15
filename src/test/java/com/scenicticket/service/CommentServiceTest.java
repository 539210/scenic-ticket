package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dao.mysql.UserDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.Item;
import com.scenicticket.model.User;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentServiceTest {
    @Test
    void requiresPaidNonRefundedOrderAtSubmissionTime() {
        Fixture fixture = new Fixture();
        fixture.orders.eligible = false;

        assertThrows(BusinessException.class,
                () -> fixture.service.submit(2L, 7L, "很好", 5, "127.0.0.1"));

        assertEquals(null, fixture.comments.saved);
    }

    @Test
    void createsThenUpdatesSingleCommentWithoutTags() {
        Fixture fixture = new Fixture();

        var created = fixture.service.submit(2L, 7L, "  首次评论  ", 5, "127.0.0.1");
        var updated = fixture.service.submit(2L, 7L, "更新后的评论", 4, "127.0.0.1");

        assertFalse(created.updated());
        assertTrue(updated.updated());
        assertEquals("更新后的评论", fixture.comments.saved.getString("content"));
        assertEquals(4, fixture.comments.saved.getInteger("rating"));
        assertFalse(fixture.comments.saved.containsKey("tags"));
        assertEquals(1, fixture.comments.documentCount);
        assertEquals(List.of("COMMENT_CREATE", "COMMENT_UPDATE"), fixture.systemLogs.types);
        assertEquals("更新评论", fixture.systemLogs.details.get(1).getString("operation"));
    }

    @Test
    void listIncludesMaskedUsernameAndBothTimestampsWhileIgnoringLegacyTags() {
        Fixture fixture = new Fixture();
        Date createdAt = new Date(1000);
        Date updatedAt = new Date(2000);
        fixture.comments.saved = new Document("user_id", 2L).append("item_id", 7L)
                .append("content", "中文评论").append("rating", 5)
                .append("tags", List.of("景色好", "服务好"))
                .append("created_at", createdAt).append("updated_at", updatedAt);
        fixture.comments.documentCount = 1;

        var result = fixture.service.listForItem(2L, 7L, 20);

        assertEquals(1, result.comments().size());
        var view = result.comments().get(0);
        assertEquals("u***", view.displayUsername());
        assertEquals("中文评论", view.content());
        assertEquals(createdAt, view.createdAt());
        assertEquals(updatedAt, view.updatedAt());
    }

    @Test
    void auditFailureDoesNotUndoSavedComment() {
        Fixture fixture = new Fixture();
        fixture.logs.fail = true;

        var result = fixture.service.submit(2L, 7L, "日志故障仍保存", 5, "127.0.0.1");

        assertFalse(result.auditRecorded());
        assertTrue(result.message().contains("审计日志写入失败"));
        assertEquals("日志故障仍保存", fixture.comments.saved.getString("content"));
    }

    @Test
    void systemAuditFailureDoesNotUndoSavedComment() {
        Fixture fixture = new Fixture();
        fixture.systemLogs.fail = true;

        var result = fixture.service.submit(2L, 7L, "系统审计故障仍保存", 5, "127.0.0.1");

        assertFalse(result.auditRecorded());
        assertEquals("系统审计故障仍保存", fixture.comments.saved.getString("content"));
    }

    private static class Fixture {
        private final FakeCommentDAO comments = new FakeCommentDAO();
        private final FakeLogDAO logs = new FakeLogDAO();
        private final FakeSystemLogDAO systemLogs = new FakeSystemLogDAO();
        private final FakeOrderDAO orders = new FakeOrderDAO();
        private final CommentService service = new CommentService(comments, logs, systemLogs, orders,
                new FakeItemDAO(), new FakeUserDAO(), authorization());
    }

    private static AuthorizationService authorization() {
        return new AuthorizationService() {
            @Override
            public User requireActiveUser(long actorUserId) {
                User user = new User();
                user.setUserId(actorUserId);
                user.setStatus(1);
                return user;
            }
        };
    }

    private static class FakeCommentDAO extends CommentDAO {
        private Document saved;
        private int documentCount;
        @Override
        public Document findByUserAndItem(long userId, long itemId) { return saved; }
        @Override
        public Document upsertComment(long userId, long itemId, String content, int rating) {
            Date created = saved == null ? new Date(1000) : saved.getDate("created_at");
            saved = new Document("user_id", userId).append("item_id", itemId).append("content", content)
                    .append("rating", rating).append("created_at", created)
                    .append("updated_at", new Date(2000));
            documentCount = 1;
            return saved;
        }
        @Override
        public List<Document> findByItemId(long itemId, int limit) { return saved == null ? List.of() : List.of(saved); }
        @Override
        public Document aggregateRatingByItem(long itemId) { return new Document("comment_count", documentCount); }
    }

    private static class FakeOrderDAO extends OrderDAO {
        private boolean eligible = true;
        @Override
        public boolean existsPaidOrder(long userId, long itemId) { return eligible; }
    }

    private static class FakeItemDAO extends ItemDAO {
        @Override
        public Optional<Item> findById(long itemId) {
            Item item = new Item(); item.setItemId(itemId); return Optional.of(item);
        }
    }

    private static class FakeUserDAO extends UserDAO {
        @Override
        public Optional<User> findById(long userId) {
            User user = new User(); user.setUserId(userId); user.setUsername("user002"); return Optional.of(user);
        }
    }

    private static class FakeLogDAO extends LogDAO {
        private boolean fail;
        @Override
        public void recordAction(long userId, long itemId, String actionType, int durationSeconds,
                                 String clientType, String ip) {
            if (fail) throw new IllegalStateException("Mongo unavailable");
        }
    }

    private static class FakeSystemLogDAO extends SystemLogDAO {
        private boolean fail;
        private final List<String> types = new java.util.ArrayList<>();
        private final List<Document> details = new java.util.ArrayList<>();
        @Override
        public void record(long userId, String logType, String logLevel, String message, Document actionDetail) {
            if (fail) throw new IllegalStateException("System audit unavailable");
            types.add(logType);
            details.add(actionDetail);
        }
    }
}
