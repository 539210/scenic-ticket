package com.scenicticket.service;

import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.util.SecurityUtil;
import org.bson.Document;

import java.util.Date;
import java.util.List;

public class SystemLogService {
    private final SystemLogDAO systemLogDAO;

    public SystemLogService() {
        this(new SystemLogDAO());
    }

    public SystemLogService(SystemLogDAO systemLogDAO) {
        this.systemLogDAO = systemLogDAO;
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

    public List<Document> queryAuditLogs(Long userId, String logType, String logLevel,
                                         Date startTime, Date endTime, int limit) {
        return systemLogDAO.findByCondition(userId, logType, logLevel, startTime, endTime, normalizeLimit(limit));
    }

    public List<Document> queryRecentLogs(int limit) {
        return systemLogDAO.findRecent(normalizeLimit(limit));
    }

    public List<Document> getAuditSummary(Date startTime, Date endTime) {
        return systemLogDAO.aggregateAuditSummary(startTime, endTime);
    }

    public List<Document> getDailyAuditTrend(Date startTime, Date endTime) {
        return systemLogDAO.aggregateDailyAuditTrend(startTime, endTime);
    }

    public List<Document> getUserOperationSummary(Date startTime, Date endTime, int limit) {
        return systemLogDAO.aggregateUserOperationSummary(startTime, endTime, normalizeLimit(limit));
    }

    private void record(long userId, String logType, String logLevel, String message, String ip, String operation) {
        if (userId <= 0) {
            throw new BusinessException("User id must be positive.");
        }
        String safeLogType = SecurityUtil.requireText(logType, "Log type", 50);
        String safeMessage = SecurityUtil.requireText(message, "System log message", 500);
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
