package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dto.StatisticsReportDTO;
import org.bson.Document;

import java.util.Date;
import java.util.List;

public class StatisticsService {
    private final LogDAO logDAO;
    private final CommentDAO commentDAO;

    public StatisticsService() {
        this(new LogDAO(), new CommentDAO());
    }

    public StatisticsService(LogDAO logDAO, CommentDAO commentDAO) {
        this.logDAO = logDAO;
        this.commentDAO = commentDAO;
    }

    public List<Document> getUserBehaviorReport(long userId, Date startTime, Date endTime) {
        return logDAO.aggregateUserBehavior(userId, startTime, endTime);
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

    public StatisticsReportDTO buildDashboardReport(Date startTime, Date endTime) {
        StatisticsReportDTO report = new StatisticsReportDTO();
        report.setHotItems(getHotItemRanking(startTime, endTime, 10));
        report.setActionTypeSummary(getActionTypeSummary(startTime, endTime));
        report.setDailyTrend(getDailyActionTrend(startTime, endTime));
        report.setHotTags(getHotTags(10));
        return report;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, 100);
    }
}
