package com.scenicticket.ui;

public final class SessionTaskGuard {
    private long generation;

    public long currentToken() {
        return generation;
    }

    public long advanceSession() {
        generation += 1;
        return generation;
    }

    public boolean isCurrent(long token) {
        return token == generation;
    }
}
