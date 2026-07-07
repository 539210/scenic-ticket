package com.scenicticket.util;

import com.scenicticket.exception.BusinessException;

import java.util.regex.Pattern;

public final class SecurityUtil {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern IP_PATTERN = Pattern.compile("^[0-9A-Fa-f:.]{3,45}$");

    private SecurityUtil() {
    }

    public static String requireText(String value, String fieldName, int maxLength) {
        String normalized = normalizeText(value, maxLength);
        if (normalized == null || normalized.isBlank()) {
            throw new BusinessException(fieldName + " is required.");
        }
        return normalized;
    }

    public static String normalizeText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (maxLength > 0 && trimmed.length() > maxLength) {
            throw new BusinessException("Input is too long: max " + maxLength + " characters.");
        }
        return trimmed;
    }

    public static String normalizeEmail(String email) {
        String normalized = requireText(email, "Email", 100);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("Valid email is required.");
        }
        return normalized;
    }

    public static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "127.0.0.1";
        }
        String normalized = normalizeText(ip, 45);
        return IP_PATTERN.matcher(normalized).matches() ? normalized : "127.0.0.1";
    }

    public static int normalizeLimit(int limit, int defaultValue, int maxValue) {
        if (limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, maxValue);
    }

    public static int normalizeOffset(int offset) {
        return Math.max(offset, 0);
    }
}
