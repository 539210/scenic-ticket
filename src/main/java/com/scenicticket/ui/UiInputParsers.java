package com.scenicticket.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;

public final class UiInputParsers {
    private UiInputParsers() {
    }

    public static Long optionalLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ID \u5fc5\u987b\u662f\u6570\u5b57\uff0c\u8bf7\u68c0\u67e5\u8f93\u5165\u5185\u5bb9", exception);
        }
    }

    public static long requiredLong(String value, String fieldName) {
        requireText(value, fieldName);
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + "\u5fc5\u987b\u662f\u6570\u5b57", exception);
        }
    }

    public static int requiredInt(String value, String fieldName) {
        requireText(value, fieldName);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + "\u5fc5\u987b\u662f\u6574\u6570", exception);
        }
    }

    public static int optionalInt(String value, int defaultValue, String fieldName) {
        return value == null || value.isBlank() ? defaultValue : requiredInt(value, fieldName);
    }

    public static BigDecimal requiredAmount(String value, String fieldName) {
        requireText(value, fieldName);
        try {
            BigDecimal amount = new BigDecimal(value.trim());
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(fieldName + "\u4e0d\u80fd\u5c0f\u4e8e 0");
            }
            return amount;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + "\u5fc5\u987b\u662f\u6709\u6548\u91d1\u989d", exception);
        }
    }

    public static LocalDate requiredDate(String value, String fieldName) {
        requireText(value, fieldName);
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + "\u5fc5\u987b\u4f7f\u7528 yyyy-MM-dd \u683c\u5f0f", exception);
        }
    }

    public static Date optionalStartDate(String value, String fieldName) {
        return optionalStartDate(value, fieldName, ZoneId.systemDefault());
    }

    public static Date optionalEndDate(String value, String fieldName) {
        return optionalEndDate(value, fieldName, ZoneId.systemDefault());
    }

    static Date optionalStartDate(String value, String fieldName, ZoneId zoneId) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Date.from(requiredDate(value, fieldName).atStartOfDay(zoneId).toInstant());
    }

    static Date optionalEndDate(String value, String fieldName, ZoneId zoneId) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Date.from(requiredDate(value, fieldName).plusDays(1).atStartOfDay(zoneId)
                .minusNanos(1_000_000).toInstant());
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "\u4e0d\u80fd\u4e3a\u7a7a");
        }
    }
}
