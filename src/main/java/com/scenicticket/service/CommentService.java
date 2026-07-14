package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.OrderDAO;
import com.scenicticket.dao.mysql.UserDAO;
import com.scenicticket.dto.CommentListDTO;
import com.scenicticket.dto.CommentSubmissionResult;
import com.scenicticket.dto.CommentViewDTO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.User;
import com.scenicticket.util.SecurityUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommentService.class);
    private final CommentDAO commentDAO;
    private final LogDAO logDAO;
    private final SystemLogDAO systemLogDAO;
    private final OrderDAO orderDAO;
    private final ItemDAO itemDAO;
    private final UserDAO userDAO;
    private final AuthorizationService authorizationService;

    public CommentService() {
        this(new CommentDAO(), new LogDAO(), new SystemLogDAO(), new OrderDAO(), new ItemDAO(), new UserDAO(),
                new AuthorizationService());
    }

    public CommentService(CommentDAO commentDAO, LogDAO logDAO, SystemLogDAO systemLogDAO,
                          OrderDAO orderDAO, ItemDAO itemDAO,
                          UserDAO userDAO, AuthorizationService authorizationService) {
        this.commentDAO = commentDAO;
        this.logDAO = logDAO;
        this.systemLogDAO = systemLogDAO;
        this.orderDAO = orderDAO;
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
        this.authorizationService = authorizationService;
    }

    public CommentSubmissionResult submit(long actorUserId, long itemId, String content, int rating,
                                          List<String> tags, String ip) {
        authorizationService.requireActiveUser(actorUserId);
        if (itemId <= 0) throw new BusinessException("景点ID必须大于 0");
        itemDAO.findById(itemId).orElseThrow(() -> new BusinessException("景点不存在"));
        if (!orderDAO.existsPaidOrder(actorUserId, itemId)) {
            throw new BusinessException("只有购买过该景点且订单未退款的用户才能评论");
        }
        if (rating < 1 || rating > 5) throw new BusinessException("评分必须在 1 到 5 之间");
        String safeContent = SecurityUtil.requireText(content, "评论内容", 1000);
        List<String> safeTags = normalizeTags(tags);
        boolean updated = commentDAO.findByUserAndItem(actorUserId, itemId) != null;
        commentDAO.upsertComment(actorUserId, itemId, safeContent, rating, safeTags);
        boolean audited = safeAudit(actorUserId, itemId, updated, rating, safeTags, safeContent.length(), ip);
        String message = updated ? "评论已更新" : "评论已发布";
        return new CommentSubmissionResult(updated, audited,
                audited ? message : message + "；但审计日志写入失败");
    }

    public CommentListDTO listForItem(long actorUserId, long itemId, int limit) {
        authorizationService.requireActiveUser(actorUserId);
        if (itemId <= 0) throw new BusinessException("景点ID必须大于 0");
        List<CommentViewDTO> views = new ArrayList<>();
        for (Document document : commentDAO.findByItemId(itemId, SecurityUtil.normalizeLimit(limit, 20, 100))) {
            long userId = numericId(document.get("user_id"));
            String username = userDAO.findById(userId).map(User::getUsername).orElse("未知用户");
            Object content = document.get("content");
            views.add(new CommentViewDTO(userId, maskUsername(username), number(document.get("rating")),
                    SecurityUtil.normalizeText(content == null ? "" : String.valueOf(content), 1000),
                    stringList(document.get("tags")), date(document.get("created_at")), date(document.get("updated_at"))));
        }
        return new CommentListDTO(commentDAO.aggregateRatingByItem(itemId), views);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return List.of();
        List<String> result = new ArrayList<>();
        for (String tag : tags) {
            String safe = SecurityUtil.normalizeText(tag, 30);
            if (safe != null && !safe.isBlank() && !result.contains(safe)) result.add(safe);
            if (result.size() > 10) throw new BusinessException("评论标签最多 10 个");
        }
        return List.copyOf(result);
    }

    private boolean safeAudit(long userId, long itemId, boolean updated, int rating,
                              List<String> tags, int contentLength, String ip) {
        String safeIp = SecurityUtil.normalizeIp(ip);
        boolean behaviorRecorded = true;
        try {
            logDAO.recordAction(userId, itemId, "COMMENT", 0, "SWING", safeIp);
        } catch (RuntimeException exception) {
            behaviorRecorded = false;
            LOGGER.warn("Comment saved but behavior audit failed for user {} item {}", userId, itemId, exception);
        }
        boolean systemAuditRecorded = true;
        String action = updated ? "COMMENT_UPDATE" : "COMMENT_CREATE";
        Document detail = new Document("actor_user_id", userId)
                .append("item_id", itemId)
                .append("rating", rating)
                .append("tags", tags)
                .append("content_length", contentLength)
                .append("operation", updated ? "更新评论" : "发表评论")
                .append("ip", safeIp)
                .append("business_key", "item:" + itemId);
        try {
            systemLogDAO.record(userId, action, "INFO", updated ? "用户更新评论" : "用户发表评论", detail);
        } catch (RuntimeException exception) {
            systemAuditRecorded = false;
            LOGGER.warn("Comment saved but system audit failed for user {} item {}", userId, itemId, exception);
        }
        return behaviorRecorded && systemAuditRecorded;
    }

    private long numericId(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException exception) { return 0L; }
    }
    private int number(Object value) { return value instanceof Number number ? number.intValue() : 0; }
    private Date date(Object value) { return value instanceof Date date ? date : null; }
    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(String::valueOf).toList();
    }
    private String maskUsername(String username) {
        if (username == null || username.isBlank()) return "未知用户";
        return username.substring(0, 1) + "***";
    }
}
