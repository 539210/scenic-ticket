package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dto.BehaviorLogQuery;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.util.SecurityUtil;
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
        logDAO.recordAction(userId, itemId, "VIEW", Math.max(durationSeconds, 0), "SWING", SecurityUtil.normalizeIp(ip));
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
                .append("keyword", SecurityUtil.normalizeText(keyword, 100))
                .append("client_info", new Document("client_type", "SWING").append("ip", SecurityUtil.normalizeIp(ip)));
        logDAO.insertActionLog(actionLog);
    }

    public void addComment(long userId, long itemId, String content, int rating, String ip) {
        throw new BusinessException("旧评论接口已停用，请使用带购买资格校验的 CommentService");
    }

    public List<Document> queryRecentLogs(BehaviorLogQuery query) {
        int limit = normalizeLimit(query == null ? 50 : query.getLimit());
        if (query == null || query.getUserId() == null) {
            throw new BusinessException("查询日志时用户ID不能为空");
        }
        return logDAO.findRecentByUserId(query.getUserId(), limit);
    }

    public List<Document> queryRecentComments(long itemId, int limit) {
        if (itemId <= 0) {
            throw new BusinessException("景点ID必须大于 0");
        }
        return commentDAO.findByItemId(itemId, normalizeLimit(limit));
    }

    private void validateIds(long userId, long itemId) {
        if (userId <= 0) {
            throw new BusinessException("用户ID必须大于 0");
        }
        if (itemId <= 0) {
            throw new BusinessException("景点ID必须大于 0");
        }
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 200);
    }
}
