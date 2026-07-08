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
    private final SystemLogService service = new SystemLogService(systemLogDAO);

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
        service.queryRecentLogs(9999);
        assertEquals(500, systemLogDAO.recentLimit);

        service.queryAuditLogs(1L, "LOGIN", "INFO", null, null, -1);
        assertEquals(50, systemLogDAO.conditionLimit);
    }

    private static class CapturingSystemLogDAO extends SystemLogDAO {
        private long userId;
        private String logType;
        private String logLevel;
        private String message;
        private Document actionDetail;
        private int recentLimit;
        private int conditionLimit;

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
    }
}
