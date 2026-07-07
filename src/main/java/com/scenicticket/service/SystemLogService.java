package com.scenicticket.service;

import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.exception.BusinessException;
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
        if (logType == null || logType.isBlank()) {
            throw new BusinessException("Log type is required.");
        }
        if (message == null || message.isBlank()) {
            throw new BusinessException("System log message is required.");
        }
        Document actionDetail = new Document()
                .append("ip", ip == null || ip.isBlank() ? "127.0.0.1" : ip)
                .append("operation", operation == null || operation.isBlank() ? logType.trim() : operation.trim());
        systemLogDAO.record(userId, logType.trim(), logLevel, message.trim(), actionDetail);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 500);
    }
}
