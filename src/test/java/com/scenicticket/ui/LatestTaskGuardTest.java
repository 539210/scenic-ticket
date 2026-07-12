package com.scenicticket.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatestTaskGuardTest {
    @Test
    void newerRequestInvalidatesOnlyOlderRequestWithSameName() {
        LatestTaskGuard guard = new LatestTaskGuard();

        long firstRefresh = guard.nextToken("refresh-orders");
        long profileRefresh = guard.nextToken("refresh-profile");
        long secondRefresh = guard.nextToken("refresh-orders");

        assertFalse(guard.isCurrent("refresh-orders", firstRefresh));
        assertTrue(guard.isCurrent("refresh-orders", secondRefresh));
        assertTrue(guard.isCurrent("refresh-profile", profileRefresh));
    }

    @Test
    void rejectsBlankTaskNames() {
        LatestTaskGuard guard = new LatestTaskGuard();

        assertThrows(IllegalArgumentException.class, () -> guard.nextToken(" "));
        assertThrows(IllegalArgumentException.class, () -> guard.isCurrent(null, 1L));
    }
}
