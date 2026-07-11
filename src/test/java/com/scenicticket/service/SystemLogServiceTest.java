package com.scenicticket.service;

import com.scenicticket.dao.mongo.SystemLogDAO;
import com.scenicticket.exception.BusinessException;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SystemLogServiceTest {
    private final CapturingSystemLogDAO systemLogDAO = new CapturingSystemLogDAO();
    private final SystemLogService service = new SystemLogService(systemLogDAO, allowAdmin());

    @Test
    void recordOperationNormalizesIpAndDefaultsBlankOperationToLogType() {
        service.recordOperation(9L, "LOGIN", "  success  ", "bad ip", "   ");

        assertEquals(9L, systemLogDAO.userId);
        assertEquals("LOGIN", systemLogDAO.logType);
        assertEquals("INFO", systemLogDAO.logLevel);
        assertEquals("success", systemLogDAO.message);
        assertEquals("127.0.0.1", systemLogDAO.actionDetail.getString("ip"));
        assertEquals("LOGIN", systemLogDAO.actionDetail.getString("operation"));
    }

    @Test
    void recordRejectsInvalidUserAndOverlongMessage() {
        assertThrows(BusinessException.class,
                () -> service.recordOperation(0L, "LOGIN", "success", null, null));
        assertThrows(BusinessException.class,
                () -> service.recordOperation(1L, "LOGIN", "x".repeat(501), null, null));
    }

    @Test
    void queryMethodsClampLimit() {
        service.queryRecentLogs(1L, 9999);
        assertEquals(500, systemLogDAO.recentLimit);

        service.queryAuditLogs(1L, 1L, " LOGIN ", " INFO ", null, null, " success ", -1);
        assertEquals(50, systemLogDAO.conditionLimit);
        assertEquals("LOGIN", systemLogDAO.conditionLogType);
        assertEquals("INFO", systemLogDAO.conditionLogLevel);
        assertEquals("success", systemLogDAO.conditionKeyword);
    }

    @Test
    void queryRejectsInvalidDateRangeBeforeDaoCall() {
        Date start = new Date(2_000L);
        Date end = new Date(1_000L);

        assertThrows(BusinessException.class,
                () -> service.queryAuditLogs(1L, 1L, "LOGIN", "INFO", start, end, "x", 20));
        assertEquals(0, systemLogDAO.conditionLimit);
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

    private static class CapturingSystemLogDAO extends SystemLogDAO {
        private long userId;
        private String logType;
        private String logLevel;
        private String message;
        private Document actionDetail;
        private int recentLimit;
        private int conditionLimit;
        private String conditionLogType;
        private String conditionLogLevel;
        private String conditionKeyword;

        @Override
        public void record(long userId, String logType, String logLevel, String message, Document actionDetail) {
            this.userId = userId;
            this.logType = logType;
            this.logLevel = logLevel;
            this.message = message;
            this.actionDetail = actionDetail;
        }

        @Override
        public List<Document> findRecent(int limit) {
            recentLimit = limit;
            return List.of();
        }

        @Override
        public List<Document> findByCondition(Long userId, String logType, String logLevel,
                                              Date startTime, Date endTime, int limit) {
            conditionLimit = limit;
            return List.of();
        }

        @Override
        public List<Document> findByCondition(Long userId, String logType, String logLevel,
                                              Date startTime, Date endTime, String keyword, int limit) {
            conditionLimit = limit;
            conditionLogType = logType;
            conditionLogLevel = logLevel;
            conditionKeyword = keyword;
            return List.of();
        }
    }
}
