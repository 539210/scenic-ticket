package com.scenicticket.ui;

import com.scenicticket.exception.BusinessException;
import com.scenicticket.exception.DBException;

import java.math.BigDecimal;
import java.sql.SQLException;
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
            return "无优惠";
        }
        return "减免" + value.stripTrailingZeros().toPlainString() + "%";
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

    public static String ratingScore(Double score) {
        if (score == null || score <= 0) {
            return "暂无评分";
        }
        return String.format("%.1f / 5", Math.max(0.0, Math.min(5.0, score)));
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

    public static BigDecimal discountedUnitPrice(BigDecimal price, BigDecimal discount) {
        return orderAmount(price, discount, 1);
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
        if (containsSchemaMismatch(throwable)) {
            return "数据库结构未升级，请联系管理员执行数据库迁移";
        }
        if (containsCause(throwable, DBException.class) || containsCause(throwable, SQLException.class)
                || containsClassName(throwable, "Mongo")) {
            return "数据库操作失败，请检查数据库服务或稍后重试";
        }
        if (containsCause(throwable, SecurityException.class)) {
            return "权限不足，无法完成该操作";
        }
        return "操作未完成，请稍后重试";
    }

    public static String readableText(Object value, String fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        long questionMarks = text.chars().filter(character -> character == '?').count();
        if (questionMarks >= 3 || text.contains("??")) {
            return "历史数据编码异常，暂无法显示";
        }
        return text;
    }

    private static boolean containsCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean containsClassName(Throwable throwable, String text) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getClass().getName().contains(text)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean containsSchemaMismatch(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                int errorCode = sqlException.getErrorCode();
                if (sqlState != null && sqlState.startsWith("42")
                        && (errorCode == 1054 || errorCode == 1146)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
