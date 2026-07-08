package com.scenicticket.util;

import com.scenicticket.exception.BusinessException;

import java.util.regex.Pattern;

public final class SecurityUtil {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern IP_PATTERN = Pattern.compile("^[0-9A-Fa-f:.]{3,45}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private SecurityUtil() {
    }

    public static String requireText(String value, String fieldName, int maxLength) {
        String normalized = normalizeText(value, maxLength);
        if (normalized == null || normalized.isBlank()) {
            throw new BusinessException(fieldName + "不能为空");
        }
        return normalized;
    }

    public static String normalizeText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (maxLength > 0 && trimmed.length() > maxLength) {
            throw new BusinessException("输入内容过长，最多 " + maxLength + " 个字符");
        }
        return trimmed;
    }

    public static String normalizeEmail(String email) {
        String normalized = requireText(email, "邮箱", 100);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
        return normalized;
    }

    public static String normalizePhone(String phone) {
        String normalized = requireText(phone, "手机号", 20);
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("手机号格式不正确，请输入 11 位大陆手机号");
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
