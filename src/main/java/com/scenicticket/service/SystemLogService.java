package com.scenicticket.service;

import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.util.SecurityUtil;
import org.bson.Document;

import java.util.Date;
import java.util.List;

public class SystemLogService {
    private final SystemLogDAO systemLogDAO;
    private final AuthorizationService authorizationService;

    public SystemLogService() {
        this(new SystemLogDAO(), new AuthorizationService());
    }

    public SystemLogService(SystemLogDAO systemLogDAO) {
        this(systemLogDAO, new AuthorizationService());
    }

    public SystemLogService(SystemLogDAO systemLogDAO, AuthorizationService authorizationService) {
        this.systemLogDAO = systemLogDAO;
        this.authorizationService = authorizationService;
    }

    public void recordOperation(long userId, String logType, String message, String ip, String operation) {
        record(userId, logType, "INFO", message, ip, operation);
    }

    public void recordWarning(long userId, String logType, String message, String ip, String operation) {
        record(userId, logType, "WARN", message, ip, operation);
    }

    public void recordError(long userId, String logType, String message, String ip, String operation) {
        record(userId, logType, "ERROR", message, ip, operation);
    }

    public List<Document> queryAuditLogs(long actorUserId, Long userId, String logType, String logLevel,
                                         Date startTime, Date endTime, int limit) {
        authorizationService.requireAdmin(actorUserId);
        return systemLogDAO.findByCondition(userId, logType, logLevel, startTime, endTime, normalizeLimit(limit));
    }

    public List<Document> queryRecentLogs(long actorUserId, int limit) {
        authorizationService.requireAdmin(actorUserId);
        return systemLogDAO.findRecent(normalizeLimit(limit));
    }

    public List<Document> getAuditSummary(long actorUserId, Date startTime, Date endTime) {
        authorizationService.requireAdmin(actorUserId);
        return systemLogDAO.aggregateAuditSummary(startTime, endTime);
    }

    public List<Document> getDailyAuditTrend(long actorUserId, Date startTime, Date endTime) {
        authorizationService.requireAdmin(actorUserId);
        return systemLogDAO.aggregateDailyAuditTrend(startTime, endTime);
    }

    public List<Document> getUserOperationSummary(long actorUserId, Date startTime, Date endTime, int limit) {
        authorizationService.requireAdmin(actorUserId);
        return systemLogDAO.aggregateUserOperationSummary(startTime, endTime, normalizeLimit(limit));
    }

    private void record(long userId, String logType, String logLevel, String message, String ip, String operation) {
        if (userId <= 0) {
            throw new BusinessException("用户ID必须大于 0");
        }
        String safeLogType = SecurityUtil.requireText(logType, "日志类型", 50);
        String safeMessage = SecurityUtil.requireText(message, "系统日志内容", 500);
        String safeOperation = SecurityUtil.normalizeText(operation, 120);
        Document actionDetail = new Document()
                .append("ip", SecurityUtil.normalizeIp(ip))
                .append("operation", safeOperation == null || safeOperation.isBlank() ? safeLogType : safeOperation);
        systemLogDAO.record(userId, safeLogType, logLevel, safeMessage, actionDetail);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 500);
    }
}
