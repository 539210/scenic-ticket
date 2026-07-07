package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dto.BehaviorLogQuery;
import com.scenicticket.exception.BusinessException;
import org.bson.Document;

import java.util.List;

public class BehaviorLogService {
    private final LogDAO logDAO;
    private final CommentDAO commentDAO;

    public BehaviorLogService() {
        this(new LogDAO(), new CommentDAO());
    }

    public BehaviorLogService(LogDAO logDAO, CommentDAO commentDAO) {
        this.logDAO = logDAO;
        this.commentDAO = commentDAO;
    }

    public void recordView(long userId, long itemId, int durationSeconds, String ip) {
        validateIds(userId, itemId);
        logDAO.recordAction(userId, itemId, "VIEW", Math.max(durationSeconds, 0), "SWING", defaultIp(ip));
    }

    public void recordSearch(long userId, String keyword, String ip) {
        if (userId <= 0) {
            throw new BusinessException("User id must be positive.");
        }
        Document actionLog = new Document()
                .append("user_id", userId)
                .append("item_id", 0L)
                .append("action_type", "SEARCH")
                .append("duration_seconds", 0)
                .append("keyword", keyword)
                .append("client_info", new Document("client_type", "SWING").append("ip", defaultIp(ip)));
        logDAO.insertActionLog(actionLog);
    }

    public void addComment(long userId, long itemId, String content, int rating, List<String> tags, String ip) {
        validateIds(userId, itemId);
        if (content == null || content.isBlank()) {
            throw new BusinessException("Comment content is required.");
        }
        if (rating < 1 || rating > 5) {
            throw new BusinessException("Rating must be between 1 and 5.");
        }
        commentDAO.addComment(userId, itemId, content.trim(), rating, tags);
        logDAO.recordAction(userId, itemId, "COMMENT", 0, "SWING", defaultIp(ip));
    }

    public List<Document> queryRecentLogs(BehaviorLogQuery query) {
        int limit = normalizeLimit(query == null ? 50 : query.getLimit());
        if (query == null || query.getUserId() == null) {
            throw new BusinessException("User id is required for recent log query.");
        }
        return logDAO.findRecentByUserId(query.getUserId(), limit);
    }

    public List<Document> queryRecentComments(long itemId, int limit) {
        if (itemId <= 0) {
            throw new BusinessException("Item id must be positive.");
        }
        return commentDAO.findByItemId(itemId, normalizeLimit(limit));
    }

    private void validateIds(long userId, long itemId) {
        if (userId <= 0) {
            throw new BusinessException("User id must be positive.");
        }
        if (itemId <= 0) {
            throw new BusinessException("Item id must be positive.");
        }
    }

    private String defaultIp(String ip) {
        return ip == null || ip.isBlank() ? "127.0.0.1" : ip;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }
}
