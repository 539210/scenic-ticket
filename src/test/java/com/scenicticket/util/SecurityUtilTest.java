package com.scenicticket.util;

import com.scenicticket.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityUtilTest {
    @Test
    void requireTextTrimsAndRejectsBlankInput() {
        assertEquals("West Lake", SecurityUtil.requireText("  West Lake  ", "Name", 20));
        assertThrows(BusinessException.class, () -> SecurityUtil.requireText("   ", "Name", 20));
    }

    @Test
    void normalizeTextRejectsOverlongInput() {
        assertThrows(BusinessException.class, () -> SecurityUtil.normalizeText("abcdef", 5));
    }

    @Test
    void normalizeEmailRejectsInvalidEmail() {
        assertEquals("user@example.com", SecurityUtil.normalizeEmail(" user@example.com "));
        assertThrows(BusinessException.class, () -> SecurityUtil.normalizeEmail("not-an-email"));
    }

    @Test
    void normalizePhoneRequiresValidMainlandMobileNumber() {
        assertEquals("13900000000", SecurityUtil.normalizePhone(" 13900000000 "));
        assertThrows(BusinessException.class, () -> SecurityUtil.normalizePhone("12345"));
        assertThrows(BusinessException.class, () -> SecurityUtil.normalizePhone(""));
    }

    @Test
    void normalizeIpFallsBackForMissingOrInvalidIp() {
        assertEquals("127.0.0.1", SecurityUtil.normalizeIp(null));
        assertEquals("127.0.0.1", SecurityUtil.normalizeIp("invalid ip"));
        assertEquals("192.168.1.5", SecurityUtil.normalizeIp(" 192.168.1.5 "));
    }

    @Test
    void normalizePaginationBoundsValues() {
        assertEquals(20, SecurityUtil.normalizeLimit(0, 20, 100));
        assertEquals(100, SecurityUtil.normalizeLimit(500, 20, 100));
        assertEquals(12, SecurityUtil.normalizeLimit(12, 20, 100));
        assertEquals(0, SecurityUtil.normalizeOffset(-10));
        assertEquals(15, SecurityUtil.normalizeOffset(15));
    }
}
