package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.ReportDAO;
import com.scenicticket.dto.AuditLogQuery;
import com.scenicticket.dto.HotItemRankingDTO;
import com.scenicticket.dto.MonthlyOrderReportDTO;
import com.scenicticket.dto.StatisticsReportDTO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.Item;
import org.bson.Document;

import java.util.Date;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatisticsService {
    private final LogDAO logDAO;
    private final CommentDAO commentDAO;
    private final ReportDAO reportDAO;
    private final SystemLogDAO systemLogDAO;
    private final AuthorizationService authorizationService;
    private final ItemDAO itemDAO;

    public StatisticsService() {
        this(new LogDAO(), new CommentDAO(), new ReportDAO(), new SystemLogDAO());
    }

    public StatisticsService(LogDAO logDAO, CommentDAO commentDAO) {
        this(logDAO, commentDAO, new ReportDAO(), new SystemLogDAO());
    }

    public StatisticsService(LogDAO logDAO, CommentDAO commentDAO, ReportDAO reportDAO, SystemLogDAO systemLogDAO) {
        this(logDAO, commentDAO, reportDAO, systemLogDAO, new AuthorizationService());
    }

    public StatisticsService(LogDAO logDAO, CommentDAO commentDAO, ReportDAO reportDAO, SystemLogDAO systemLogDAO,
                             AuthorizationService authorizationService) {
        this(logDAO, commentDAO, reportDAO, systemLogDAO, authorizationService, new ItemDAO());
    }

    public StatisticsService(LogDAO logDAO, CommentDAO commentDAO, ReportDAO reportDAO, SystemLogDAO systemLogDAO,
                             AuthorizationService authorizationService, ItemDAO itemDAO) {
        this.logDAO = logDAO;
        this.commentDAO = commentDAO;
        this.reportDAO = reportDAO;
        this.systemLogDAO = systemLogDAO;
        this.authorizationService = authorizationService;
        this.itemDAO = itemDAO;
    }

    public List<Document> getUserBehaviorReport(long actorUserId, long userId, Date startTime, Date endTime) {
        authorizationService.requireSelfOrAdmin(actorUserId, userId);
        validateUserId(userId);
        return logDAO.aggregateUserBehavior(userId, startTime, endTime);
    }

    public Document getUserReport(long actorUserId, long userId, Date startTime, Date endTime) {
        authorizationService.requireSelfOrAdmin(actorUserId, userId);
        validateUserId(userId);
        Document report = logDAO.aggregateUserReport(userId, startTime, endTime);
        report.append("behavior_summary", logDAO.aggregateUserBehavior(userId, startTime, endTime));
        report.append("recent_comments", commentDAO.findByUserId(userId, 10));
        report.append("recent_actions", logDAO.findRecentByUserId(userId, 10));
        return report;
    }

    public List<HotItemRankingDTO> getHotItemRanking(Date startTime, Date endTime, int limit) {
        List<Document> hotItems = logDAO.aggregateHotItems(startTime, endTime, normalizeLimit(limit));
        List<Long> itemIds = hotItems.stream()
                .map(document -> readLong(document.get("_id")))
                .filter(itemId -> itemId != null && itemId > 0)
                .toList();
        Map<Long, Item> itemById = new LinkedHashMap<>();
        for (Item item : itemDAO.findByIds(itemIds)) {
            itemById.put(item.getItemId(), item);
        }
        return hotItems.stream()
                .map(document -> toHotItemRanking(document, itemById.get(readLong(document.get("_id")))))
                .sorted(Comparator.comparingLong(HotItemRankingDTO::getTotalActions).reversed()
                        .thenComparing(Comparator.comparingLong(HotItemRankingDTO::getViewCount).reversed())
                        .thenComparing(Comparator.comparingLong(HotItemRankingDTO::getOrderCount).reversed())
                        .thenComparingLong(HotItemRankingDTO::getItemId))
                .toList();
    }

    public List<Document> getActionTypeSummary(Date startTime, Date endTime) {
        return logDAO.aggregateActionTypeSummary(startTime, endTime).stream()
                .sorted(Comparator.comparingLong((Document document) -> readLongOrZero(document.get("action_count")))
                        .reversed()
                        .thenComparing(document -> String.valueOf(document.get("action_type"))))
                .toList();
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
        return commentDAO.aggregateHotTags(normalizeLimit(limit)).stream()
                .sorted(Comparator.comparingLong((Document document) -> readLongOrZero(document.get("tag_count")))
                        .reversed()
                        .thenComparing(document -> String.valueOf(document.get("_id"))))
                .toList();
    }

    public List<Document> getSystemAuditSummary(long actorUserId, Date startTime, Date endTime) {
        authorizationService.requireAdmin(actorUserId);
        return systemLogDAO.aggregateAuditSummary(startTime, endTime);
    }

    public List<Document> getSystemAuditTrend(long actorUserId, Date startTime, Date endTime) {
        authorizationService.requireAdmin(actorUserId);
        return systemLogDAO.aggregateDailyAuditTrend(startTime, endTime);
    }

    public List<Document> getUserOperationSummary(long actorUserId, Date startTime, Date endTime, int limit) {
        authorizationService.requireAdmin(actorUserId);
        return systemLogDAO.aggregateUserOperationSummary(startTime, endTime, normalizeLimit(limit));
    }

    public List<Document> querySystemAuditLogs(long actorUserId, Long userId, String logType, String logLevel,
                                               Date startTime, Date endTime, int limit) {
        return querySystemAuditLogs(actorUserId, userId, logType, logLevel, startTime, endTime, null, limit);
    }

    public List<Document> querySystemAuditLogs(long actorUserId, Long userId, String logType, String logLevel,
                                               Date startTime, Date endTime, String keyword, int limit) {
        AuditLogQuery query = new AuditLogQuery();
        query.setUserId(userId);
        query.setLogType(logType);
        query.setLogLevel(logLevel);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setKeyword(keyword);
        query.setLimit(limit);
        return querySystemAuditLogs(actorUserId, query);
    }

    public List<Document> querySystemAuditLogs(long actorUserId, AuditLogQuery query) {
        authorizationService.requireAdmin(actorUserId);
        AuditLogQuery safeQuery = query == null ? new AuditLogQuery() : query;
        validateTimeRange(safeQuery.getStartTime(), safeQuery.getEndTime());
        return systemLogDAO.findByCondition(
                safeQuery.getUserId(),
                normalizeOptionalText(safeQuery.getLogType()),
                normalizeOptionalText(safeQuery.getLogLevel()),
                safeQuery.getStartTime(),
                safeQuery.getEndTime(),
                normalizeOptionalText(safeQuery.getKeyword()),
                normalizeLimit(safeQuery.getLimit()));
    }

    public List<MonthlyOrderReportDTO> getMonthlyOrderReport(int year, int month) {
        validateReportMonth(year, month);
        return reportDAO.callMonthlyOrderReport(year, month).stream()
                .sorted(Comparator.comparing(MonthlyOrderReportDTO::getOrderDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public StatisticsReportDTO buildDashboardReport(long actorUserId, Date startTime, Date endTime) {
        authorizationService.requireAdmin(actorUserId);
        StatisticsReportDTO report = new StatisticsReportDTO();
        report.setHotItems(getHotItemRanking(startTime, endTime, 10));
        report.setActionTypeSummary(getActionTypeSummary(startTime, endTime));
        report.setDailyTrend(getDailyActionTrend(startTime, endTime));
        report.setHotTags(getHotTags(10));
        report.setSystemAuditSummary(getSystemAuditSummary(actorUserId, startTime, endTime));
        report.setSystemAuditTrend(getSystemAuditTrend(actorUserId, startTime, endTime));
        return report;
    }

    public StatisticsReportDTO buildDashboardReport(long actorUserId, Date startTime, Date endTime,
                                                     int reportYear, int reportMonth) {
        StatisticsReportDTO report = buildDashboardReport(actorUserId, startTime, endTime);
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

    private HotItemRankingDTO toHotItemRanking(Document document, Item item) {
        HotItemRankingDTO dto = new HotItemRankingDTO();
        Long itemId = readLong(document.get("_id"));
        dto.setItemId(itemId == null ? 0L : itemId);
        dto.setTotalActions(readLongOrZero(document.get("total_actions")));
        dto.setViewCount(readLongOrZero(document.get("view_count")));
        dto.setOrderCount(readLongOrZero(document.get("order_count")));
        dto.setAvgDuration(readDouble(document.get("avg_duration")));
        if (item == null) {
            dto.setItemTitle("景点不存在或已删除");
            dto.setCategoryName("-");
            dto.setItemFound(false);
            return dto;
        }
        dto.setItemTitle(item.getTitle());
        dto.setCategoryName(String.valueOf(item.getCategoryId()));
        dto.setItemStatus(item.getStatus());
        dto.setItemFound(true);
        return dto;
    }

    private Long readLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private long readLongOrZero(Object value) {
        Long number = readLong(value);
        return number == null ? 0L : number;
    }

    private double readDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateTimeRange(Date startTime, Date endTime) {
        if (startTime != null && endTime != null && startTime.after(endTime)) {
            throw new BusinessException("Audit start time must not be after end time.");
        }
    }
}
