package com.scenicticket.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTaskGuardTest {
    @Test
    void taskTokenBecomesStaleAfterLogoutOrAccountSwitch() {
        SessionTaskGuard guard = new SessionTaskGuard();
        guard.advanceSession();
        long firstAccountTask = guard.currentToken();

        assertTrue(guard.isCurrent(firstAccountTask));

        guard.advanceSession();

        assertFalse(guard.isCurrent(firstAccountTask));
        assertTrue(guard.isCurrent(guard.currentToken()));
    }
}
