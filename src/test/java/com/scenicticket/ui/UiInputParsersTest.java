package com.scenicticket.ui;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiInputParsersTest {
    @Test
    void parsesOptionalAndRequiredNumbersWithTrimming() {
        assertNull(UiInputParsers.optionalLong(" "));
        assertEquals(12L, UiInputParsers.optionalLong(" 12 "));
        assertEquals(7L, UiInputParsers.requiredLong(" 7 ", "用户ID"));
        assertEquals(30, UiInputParsers.requiredInt(" 30 ", "条数"));
        assertEquals(50, UiInputParsers.optionalInt("", 50, "条数"));
    }

    @Test
    void rejectsMissingMalformedAndNegativeAmounts() {
        assertThrows(IllegalArgumentException.class,
                () -> UiInputParsers.requiredLong("", "用户ID"));
        assertThrows(IllegalArgumentException.class,
                () -> UiInputParsers.optionalLong("abc"));
        assertThrows(IllegalArgumentException.class,
                () -> UiInputParsers.requiredInt("1.5", "条数"));
        assertThrows(IllegalArgumentException.class,
                () -> UiInputParsers.requiredAmount("-0.01", "票价"));
        assertEquals(new BigDecimal("12.50"), UiInputParsers.requiredAmount(" 12.50 ", "票价"));
    }

    @Test
    void parsesIsoDatesAndInclusiveAuditBoundaries() {
        assertEquals(LocalDate.of(2026, 7, 12), UiInputParsers.requiredDate("2026-07-12", "开始日期"));
        assertThrows(IllegalArgumentException.class,
                () -> UiInputParsers.requiredDate("2026-02-30", "开始日期"));

        assertEquals(Instant.parse("2026-07-12T00:00:00Z"),
                UiInputParsers.optionalStartDate("2026-07-12", "开始日期", ZoneOffset.UTC).toInstant());
        assertEquals(Instant.parse("2026-07-12T23:59:59.999Z"),
                UiInputParsers.optionalEndDate("2026-07-12", "结束日期", ZoneOffset.UTC).toInstant());
    }

    @Test
    void normalizesOptionalText() {
        assertNull(UiInputParsers.blankToNull(" \t"));
        assertEquals("hello", UiInputParsers.blankToNull(" hello "));
    }
}
