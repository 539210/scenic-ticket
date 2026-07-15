package com.scenicticket.service;

import com.scenicticket.dao.mongo.CommentDAO;
import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.dao.mysql.ItemDAO;
import com.scenicticket.dao.mysql.ReportDAO;
import com.scenicticket.dto.AuditLogQuery;
import com.scenicticket.dto.HotItemRankingDTO;
import com.scenicticket.dto.MonthlyOrderDetailDTO;
import com.scenicticket.exception.BusinessException;
import com.scenicticket.model.Item;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatisticsServiceTest {
    private final StubLogDAO logDAO = new StubLogDAO();
    private final CapturingSystemLogDAO systemLogDAO = new CapturingSystemLogDAO();
    private final StatisticsService service = new StatisticsService(logDAO, new CommentDAO(), new ReportDAO(),
            systemLogDAO, allowAdmin(), new StubItemDAO());

    @Test
    void hotRankingEnrichesMongoItemIdsWithMysqlItemNamesAndMissingState() {
        logDAO.hotItems = List.of(
                new Document("_id", 1L).append("total_actions", 12).append("view_count", 9)
                        .append("order_count", 3).append("avg_duration", 45.5),
                new Document("_id", 99L).append("total_actions", 5).append("view_count", 5)
                        .append("order_count", 0).append("avg_duration", 10.0)
        );

        List<HotItemRankingDTO> rankings = service.getHotItemRanking(null, null, 10);

        assertEquals(2, rankings.size());
        assertEquals("南山日出观景区", rankings.get(0).getItemTitle());
        assertEquals(12L, rankings.get(0).getTotalActions());
        assertEquals(9L, rankings.get(0).getViewCount());
        assertEquals(3L, rankings.get(0).getOrderCount());
        assertEquals(45.5, rankings.get(0).getAvgDuration(), 0.001);
        assertFalse(rankings.get(1).isItemFound());
        assertEquals("景点不存在或已删除", rankings.get(1).getItemTitle());
    }

    @Test
    void hotRankingUsesItemIdAsStableFinalTieBreaker() {
        logDAO.hotItems = List.of(
                new Document("_id", 99L).append("total_actions", 3).append("view_count", 3)
                        .append("order_count", 0).append("avg_duration", 20.0),
                new Document("_id", 1L).append("total_actions", 3).append("view_count", 3)
                        .append("order_count", 0).append("avg_duration", 20.0)
        );

        List<HotItemRankingDTO> rankings = service.getHotItemRanking(null, null, 10);

        assertEquals(1L, rankings.get(0).getItemId());
        assertEquals(99L, rankings.get(1).getItemId());
    }

    @Test
    void auditQueryPassesKeywordDateAndLimitToDao() {
        Date start = new Date(1_000L);
        Date end = new Date(2_000L);
        AuditLogQuery query = new AuditLogQuery();
        query.setUserId(2L);
        query.setLogType(" LOGIN ");
        query.setLogLevel(" INFO ");
        query.setStartTime(start);
        query.setEndTime(end);
        query.setKeyword(" 支付 ");
        query.setLimit(999);

        service.querySystemAuditLogs(1L, query);

        assertEquals(2L, systemLogDAO.userId);
        assertEquals("LOGIN", systemLogDAO.logType);
        assertEquals("INFO", systemLogDAO.logLevel);
        assertEquals("支付", systemLogDAO.keyword);
        assertEquals(100, systemLogDAO.limit);
        assertEquals(start, systemLogDAO.startTime);
        assertEquals(end, systemLogDAO.endTime);
    }

    @Test
    void auditQueryRejectsReversedDateRange() {
        AuditLogQuery query = new AuditLogQuery();
        query.setStartTime(new Date(2_000L));
        query.setEndTime(new Date(1_000L));

        assertThrows(BusinessException.class, () -> service.querySystemAuditLogs(1L, query));
    }

    @Test
    void monthlyOrderDetailsRequireAdminAndPassSelectedDateToDao() {
        CapturingReportDAO reportDAO = new CapturingReportDAO();
        StatisticsService detailService = new StatisticsService(logDAO, new CommentDAO(), reportDAO,
                systemLogDAO, allowAdmin(), new StubItemDAO());
        LocalDate orderDate = LocalDate.of(2026, 7, 1);

        detailService.getMonthlyOrderDetails(1L, orderDate);

        assertEquals(orderDate, reportDAO.orderDate);
    }

    private static AuthorizationService allowAdmin() {
        return new AuthorizationService(new com.scenicticket.dao.mysql.UserDAO()) {
            @Override
            public com.scenicticket.model.User requireAdmin(long actorUserId) {
                com.scenicticket.model.User user = new com.scenicticket.model.User();
                user.setUserId(actorUserId);
                user.setRole("ADMIN");
                user.setStatus(1);
                return user;
            }
        };
    }

    private static class StubLogDAO extends LogDAO {
        private List<Document> hotItems = List.of();

        @Override
        public List<Document> aggregateHotItems(Date startTime, Date endTime, int limit) {
            return hotItems;
        }
    }

    private static class StubItemDAO extends ItemDAO {
        private final Map<Long, Item> items = Map.of(1L, item(1L, "南山日出观景区"));

        @Override
        public List<Item> findByIds(List<Long> itemIds) {
            return itemIds.stream()
                    .map(items::get)
                    .filter(item -> item != null)
                    .toList();
        }

        private static Item item(long itemId, String title) {
            Item item = new Item();
            item.setItemId(itemId);
            item.setTitle(title);
            item.setCategoryId(1L);
            item.setStatus(1);
            return item;
        }
    }

    private static class CapturingReportDAO extends ReportDAO {
        private LocalDate orderDate;

        @Override
        public List<MonthlyOrderDetailDTO> findMonthlyOrderDetails(LocalDate orderDate) {
            this.orderDate = orderDate;
            return List.of();
        }
    }

    private static class CapturingSystemLogDAO extends SystemLogDAO {
        private Long userId;
        private String logType;
        private String logLevel;
        private Date startTime;
        private Date endTime;
        private String keyword;
        private int limit;

        @Override
        public List<Document> findByCondition(Long userId, String logType, String logLevel,
                                              Date startTime, Date endTime, String keyword, int limit) {
            this.userId = userId;
            this.logType = logType;
            this.logLevel = logLevel;
            this.startTime = startTime;
            this.endTime = endTime;
            this.keyword = keyword;
            this.limit = limit;
            return List.of();
        }
    }
}
