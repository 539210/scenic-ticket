package com.scenicticket.ui;

import java.util.HashMap;
import java.util.Map;

public final class LatestTaskGuard {
    private final Map<String, Long> generations = new HashMap<>();

    public synchronized long nextToken(String taskName) {
        String key = requireTaskName(taskName);
        long next = generations.getOrDefault(key, 0L) + 1L;
        generations.put(key, next);
        return next;
    }

    public synchronized boolean isCurrent(String taskName, long token) {
        return generations.getOrDefault(requireTaskName(taskName), 0L) == token;
    }

    private String requireTaskName(String taskName) {
        if (taskName == null || taskName.isBlank()) {
            throw new IllegalArgumentException("taskName must not be blank");
        }
        return taskName;
    }
}
