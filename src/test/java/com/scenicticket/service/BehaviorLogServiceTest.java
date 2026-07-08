package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dto.BehaviorLogQuery;
import com.scenicticket.exception.BusinessException;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BehaviorLogServiceTest {
    private final CapturingLogDAO logDAO = new CapturingLogDAO();
    private final CapturingCommentDAO commentDAO = new CapturingCommentDAO();
    private final BehaviorLogService service = new BehaviorLogService(logDAO, commentDAO);

    @Test
    void recordSearchNormalizesKeywordAndIp() {
        service.recordSearch(7L, "  lake ticket  ", "bad ip");

        assertEquals(7L, logDAO.inserted.getLong("user_id"));
        assertEquals("SEARCH", logDAO.inserted.getString("action_type"));
        assertEquals("lake ticket", logDAO.inserted.getString("keyword"));
        assertEquals("127.0.0.1", logDAO.inserted.get("client_info", Document.class).getString("ip"));
    }

    @Test
    void recordViewClampsNegativeDuration() {
        service.recordView(7L, 9L, -30, "192.168.1.8");

        ActionRecord record = logDAO.records.get(0);
        assertEquals(7L, record.userId);
        assertEquals(9L, record.itemId);
        assertEquals("VIEW", record.actionType);
        assertEquals(0, record.durationSeconds);
        assertEquals("192.168.1.8", record.ip);
    }

    @Test
    void addCommentTrimsContentAndRecordsAction() {
        service.addComment(3L, 4L, "  good view  ", 5, List.of("family"), "10.0.0.1");

        assertEquals("good view", commentDAO.comment.getString("content"));
        assertEquals(5, commentDAO.comment.getInteger("rating"));
        assertEquals("COMMENT", logDAO.records.get(0).actionType);
    }

    @Test
    void addCommentRejectsInvalidRatingAndBlankContent() {
        assertThrows(BusinessException.class, () -> service.addComment(1L, 2L, "", 5, List.of(), null));
        assertThrows(BusinessException.class, () -> service.addComment(1L, 2L, "ok", 6, List.of(), null));
    }

    @Test
    void queryRecentLogsRequiresUserIdAndNormalizesLimit() {
        BehaviorLogQuery query = new BehaviorLogQuery();
        query.setUserId(3L);
        query.setLimit(999);

        service.queryRecentLogs(query);

        assertEquals(3L, logDAO.queryUserId);
        assertEquals(200, logDAO.queryLimit);
        assertThrows(BusinessException.class, () -> service.queryRecentLogs(null));
    }

    private static class CapturingLogDAO extends LogDAO {
        private Document inserted;
        private long queryUserId;
        private int queryLimit;
        private final List<ActionRecord> records = new ArrayList<>();

        @Override
        public void insertActionLog(Document actionLog) {
            inserted = actionLog;
        }

        @Override
        public void recordAction(long userId, long itemId, String actionType, int durationSeconds,
                                 String clientType, String ip) {
            records.add(new ActionRecord(userId, itemId, actionType, durationSeconds, clientType, ip));
        }

        @Override
        public List<Document> findRecentByUserId(long userId, int limit) {
            queryUserId = userId;
            queryLimit = limit;
            return List.of();
        }
    }

    private static class CapturingCommentDAO extends CommentDAO {
        private Document comment;

        @Override
        public void addComment(long userId, long itemId, String content, int rating, List<String> tags) {
            comment = new Document()
                    .append("user_id", userId)
                    .append("item_id", itemId)
                    .append("content", content)
                    .append("rating", rating)
                    .append("tags", tags);
        }
    }

    private record ActionRecord(long userId, long itemId, String actionType, int durationSeconds,
                                String clientType, String ip) {
    }
}
