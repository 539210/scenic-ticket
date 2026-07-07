package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.dao.mysql.ReportDAO;
import com.scenicticket.dto.MonthlyOrderReportDTO;
import com.scenicticket.dto.StatisticsReportDTO;
import com.scenicticket.exception.BusinessException;
import org.bson.Document;

import java.util.Date;
import java.util.List;

public class StatisticsService {
    private final LogDAO logDAO;
    private final CommentDAO commentDAO;
    private final ReportDAO reportDAO;
    private final SystemLogDAO systemLogDAO;

    public StatisticsService() {
        this(new LogDAO(), new CommentDAO(), new ReportDAO(), new SystemLogDAO());
    }

    public StatisticsService(LogDAO logDAO, CommentDAO commentDAO) {
        this(logDAO, commentDAO, new ReportDAO(), new SystemLogDAO());
    }

    public StatisticsService(LogDAO logDAO, CommentDAO commentDAO, ReportDAO reportDAO, SystemLogDAO systemLogDAO) {
        this.logDAO = logDAO;
        this.commentDAO = commentDAO;
        this.reportDAO = reportDAO;
        this.systemLogDAO = systemLogDAO;
    }

    public List<Document> getUserBehaviorReport(long userId, Date startTime, Date endTime) {
        validateUserId(userId);
        return logDAO.aggregateUserBehavior(userId, startTime, endTime);
    }

    public Document getUserReport(long userId, Date startTime, Date endTime) {
        validateUserId(userId);
        Document report = logDAO.aggregateUserReport(userId, startTime, endTime);
        report.append("behavior_summary", logDAO.aggregateUserBehavior(userId, startTime, endTime));
        report.append("recent_comments", commentDAO.findByUserId(userId, 10));
        report.append("recent_actions", logDAO.findRecentByUserId(userId, 10));
        return report;
    }

    public List<Document> getHotItemRanking(Date startTime, Date endTime, int limit) {
        return logDAO.aggregateHotItems(startTime, endTime, normalizeLimit(limit));
    }

    public List<Document> getActionTypeSummary(Date startTime, Date endTime) {
        return logDAO.aggregateActionTypeSummary(startTime, endTime);
    }

    public List<Document> getDailyActionTrend(Date startTime, Date endTime) {
        return logDAO.aggregateDailyTrend(startTime, endTime);
    }

    public Document getItemRatingSummary(long itemId) {
        return commentDAO.aggregateRatingByItem(itemId);
    }

    public List<Document> getItemRatingDistribution(long itemId) {
        return commentDAO.aggregateRatingDistribution(itemId);
    }

    public List<Document> getHotTags(int limit) {
        return commentDAO.aggregateHotTags(normalizeLimit(limit));
    }

    public List<Document> getSystemAuditSummary(Date startTime, Date endTime) {
        return systemLogDAO.aggregateAuditSummary(startTime, endTime);
    }

    public List<Document> getSystemAuditTrend(Date startTime, Date endTime) {
        return systemLogDAO.aggregateDailyAuditTrend(startTime, endTime);
    }

    public List<Document> getUserOperationSummary(Date startTime, Date endTime, int limit) {
        return systemLogDAO.aggregateUserOperationSummary(startTime, endTime, normalizeLimit(limit));
    }

    public List<Document> querySystemAuditLogs(Long userId, String logType, String logLevel,
                                               Date startTime, Date endTime, int limit) {
        return systemLogDAO.findByCondition(userId, logType, logLevel, startTime, endTime, normalizeLimit(limit));
    }

    public List<MonthlyOrderReportDTO> getMonthlyOrderReport(int year, int month) {
        validateReportMonth(year, month);
        return reportDAO.callMonthlyOrderReport(year, month);
    }

    public StatisticsReportDTO buildDashboardReport(Date startTime, Date endTime) {
        StatisticsReportDTO report = new StatisticsReportDTO();
        report.setHotItems(getHotItemRanking(startTime, endTime, 10));
        report.setActionTypeSummary(getActionTypeSummary(startTime, endTime));
        report.setDailyTrend(getDailyActionTrend(startTime, endTime));
        report.setHotTags(getHotTags(10));
        report.setSystemAuditSummary(getSystemAuditSummary(startTime, endTime));
        report.setSystemAuditTrend(getSystemAuditTrend(startTime, endTime));
        return report;
    }

    public StatisticsReportDTO buildDashboardReport(Date startTime, Date endTime, int reportYear, int reportMonth) {
        StatisticsReportDTO report = buildDashboardReport(startTime, endTime);
        report.setMonthlyOrderReport(getMonthlyOrderReport(reportYear, reportMonth));
        return report;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, 100);
    }

    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw new BusinessException("User id must be positive.");
        }
    }

    private void validateReportMonth(int year, int month) {
        if (year < 2000 || year > 2100) {
            throw new BusinessException("Report year must be between 2000 and 2100.");
        }
        if (month < 1 || month > 12) {
            throw new BusinessException("Report month must be between 1 and 12.");
        }
    }
}
