package com.scenicticket.service;

import com.scenicticket.dao.mongo.LogDAO;
import com.scenicticket.dao.mongo.SystemLogDAO;
import org.bson.Document;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchLogServiceStressTest {
    private static final int TOTAL_LOGS = 10_000;
    private static final int CONCURRENCY = 50;
    private static final int LOGS_PER_WORKER = TOTAL_LOGS / CONCURRENCY;

    @Test
    void importsTenThousandActionLogsWithFiftyConcurrentWorkers() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("stressTests", "false")),
                "Set -DstressTests=true to run the Day 08 stress test.");

        ConcurrentLogDAO logDAO = new ConcurrentLogDAO();
        BatchLogService service = new BatchLogService(logDAO, new SystemLogDAO());
        CyclicBarrier startBarrier = new CyclicBarrier(CONCURRENCY);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        Instant start = Instant.now();

        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int worker = 0; worker < CONCURRENCY; worker += 1) {
                int workerId = worker;
                futures.add(executor.submit(importTask(service, startBarrier, workerId)));
            }

            int imported = 0;
            for (Future<Integer> future : futures) {
                imported += future.get(30, TimeUnit.SECONDS);
            }

            Duration elapsed = Duration.between(start, Instant.now());
            assertEquals(TOTAL_LOGS, imported);
            assertEquals(TOTAL_LOGS, logDAO.totalInserted.get());
            assertEquals(CONCURRENCY * 2, logDAO.batchSizes.size());
            assertTrue(logDAO.batchSizes.stream().allMatch(size -> size <= 100));
            assertTrue(elapsed.toSeconds() < 30, "Stress test took too long: " + elapsed);
            System.out.printf("Day08 stress test imported %,d logs with %d workers in %d ms.%n",
                    TOTAL_LOGS, CONCURRENCY, elapsed.toMillis());
        } finally {
            executor.shutdownNow();
        }
    }

    private static Callable<Integer> importTask(BatchLogService service, CyclicBarrier startBarrier, int workerId) {
        return () -> {
            List<Document> logs = new ArrayList<>(LOGS_PER_WORKER);
            int startSeq = workerId * LOGS_PER_WORKER;
            for (int i = 0; i < LOGS_PER_WORKER; i += 1) {
                logs.add(new Document()
                        .append("user_id", (long) (workerId + 1))
                        .append("item_id", (long) ((startSeq + i) % 50 + 1))
                        .append("action_type", i % 3 == 0 ? "VIEW" : "SEARCH")
                        .append("duration_seconds", i % 120)
                        .append("client_info", new Document("client_type", "STRESS").append("ip", "127.0.0.1")));
            }
            startBarrier.await(10, TimeUnit.SECONDS);
            return service.importActionLogs(logs, 100);
        };
    }

    private static class ConcurrentLogDAO extends LogDAO {
        private final AtomicInteger totalInserted = new AtomicInteger();
        private final ConcurrentLinkedQueue<Integer> batchSizes = new ConcurrentLinkedQueue<>();

        @Override
        public void insertActionLogs(List<Document> actionLogs) {
            batchSizes.add(actionLogs.size());
            totalInserted.addAndGet(actionLogs.size());
        }
    }
}
