package com.scenicticket.service;

import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.SystemLogDAO;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class BatchLogService {
    private static final int DEFAULT_BATCH_SIZE = 500;

    private final LogDAO logDAO;
    private final SystemLogDAO systemLogDAO;

    public BatchLogService() {
        this(new LogDAO(), new SystemLogDAO());
    }

    public BatchLogService(LogDAO logDAO, SystemLogDAO systemLogDAO) {
        this.logDAO = logDAO;
        this.systemLogDAO = systemLogDAO;
    }

    public int importActionLogs(List<Document> actionLogs) {
        return importActionLogs(actionLogs, DEFAULT_BATCH_SIZE);
    }

    public int importActionLogs(List<Document> actionLogs, int batchSize) {
        return importInBatches(actionLogs, normalizeBatchSize(batchSize), logDAO::insertActionLogs);
    }

    public int importSystemLogs(List<Document> systemLogs) {
        return importSystemLogs(systemLogs, DEFAULT_BATCH_SIZE);
    }

    public int importSystemLogs(List<Document> systemLogs, int batchSize) {
        return importInBatches(systemLogs, normalizeBatchSize(batchSize), systemLogDAO::insertSystemLogs);
    }

    private int importInBatches(List<Document> logs, int batchSize, java.util.function.Consumer<List<Document>> writer) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        int imported = 0;
        for (int start = 0; start < logs.size(); start += batchSize) {
            int end = Math.min(start + batchSize, logs.size());
            List<Document> batch = new ArrayList<>(logs.subList(start, end));
            writer.accept(batch);
            imported += batch.size();
        }
        return imported;
    }

    private int normalizeBatchSize(int batchSize) {
        if (batchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }
        return Math.min(batchSize, 2000);
    }
}
