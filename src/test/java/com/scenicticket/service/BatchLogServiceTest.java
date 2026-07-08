package com.scenicticket.service;

import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.SystemLogDAO;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BatchLogServiceTest {
    private final CapturingLogDAO logDAO = new CapturingLogDAO();
    private final CapturingSystemLogDAO systemLogDAO = new CapturingSystemLogDAO();
    private final BatchLogService service = new BatchLogService(logDAO, systemLogDAO);

    @Test
    void importActionLogsSplitsBatchesAndSkipsNullDocuments() {
        List<Document> logs = new ArrayList<>(documents(5));
        logs.add(2, null);

        int imported = service.importActionLogs(logs, 2);

        assertEquals(5, imported);
        assertEquals(List.of(2, 2, 1), logDAO.batchSizes());
        assertFalse(logDAO.batches.stream().flatMap(List::stream).anyMatch(document -> document == null));
    }

    @Test
    void invalidBatchSizeUsesDefaultBatchSize() {
        int imported = service.importSystemLogs(documents(1200), -1);

        assertEquals(1200, imported);
        assertEquals(List.of(500, 500, 200), systemLogDAO.batchSizes());
    }

    @Test
    void largeBatchSizeIsCapped() {
        int imported = service.importActionLogs(documents(5001), 5000);

        assertEquals(5001, imported);
        assertEquals(List.of(2000, 2000, 1001), logDAO.batchSizes());
    }

    @Test
    void emptyOrNullInputDoesNotWrite() {
        assertEquals(0, service.importActionLogs(null));
        assertEquals(0, service.importActionLogs(List.of()));
        assertEquals(0, service.importActionLogs(new ArrayList<>()));
        assertEquals(List.of(), logDAO.batchSizes());
    }

    private static List<Document> documents(int count) {
        List<Document> documents = new ArrayList<>(count);
        for (int i = 0; i < count; i += 1) {
            documents.add(new Document("seq", i));
        }
        return documents;
    }

    private static class CapturingLogDAO extends LogDAO {
        private final List<List<Document>> batches = new ArrayList<>();

        @Override
        public void insertActionLogs(List<Document> actionLogs) {
            batches.add(new ArrayList<>(actionLogs));
        }

        private List<Integer> batchSizes() {
            return batches.stream().map(List::size).toList();
        }
    }

    private static class CapturingSystemLogDAO extends SystemLogDAO {
        private final List<List<Document>> batches = new ArrayList<>();

        @Override
        public void insertSystemLogs(List<Document> systemLogs) {
            batches.add(new ArrayList<>(systemLogs));
        }

        private List<Integer> batchSizes() {
            return batches.stream().map(List::size).toList();
        }
    }
}
