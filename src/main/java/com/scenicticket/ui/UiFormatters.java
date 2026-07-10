package com.scenicticket.ui;

import com.scenicticket.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class UiFormatters {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private UiFormatters() {
    }

    public static String date(Object value) {
        if (value instanceof java.util.Date date) {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DATE_TIME);
        }
        return value == null ? "-" : String.valueOf(value);
    }

    public static String money(BigDecimal value) {
        return value == null ? "¥0.00" : "¥" + value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public static String discount(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return "无折扣";
        }
        return value.stripTrailingZeros().toPlainString() + "%";
    }

    public static String itemStatus(Integer status) {
        return status != null && status == 1 ? "上架" : "下架";
    }

    public static String orderStatus(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已取消";
            case 3 -> "已完成";
            default -> "未知";
        };
    }

    public static String role(String role) {
        return switch (role == null ? "" : role) {
            case "ADMIN" -> "管理员";
            case "USER" -> "普通用户";
            default -> "未知";
        };
    }

    public static String recommendationScore(double score) {
        return String.format("%.1f", Math.max(0.0, Math.min(100.0, score)));
    }

    public static BigDecimal orderAmount(BigDecimal price, BigDecimal discount, int quantity) {
        if (price == null || quantity <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal safeDiscount = discount == null ? BigDecimal.ZERO : discount;
        BigDecimal multiplier = BigDecimal.ONE.subtract(
                safeDiscount.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
        return price.multiply(BigDecimal.valueOf(quantity)).multiply(multiplier)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public static String chineseError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && !message.isBlank() && (current instanceof BusinessException
                    || current instanceof IllegalArgumentException || current instanceof IllegalStateException)) {
                return message;
            }
            current = current.getCause();
        }
        return "操作未完成，请检查数据库连接或稍后重试";
    }
}
