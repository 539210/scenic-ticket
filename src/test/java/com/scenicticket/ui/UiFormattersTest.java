package com.scenicticket.ui;

import com.scenicticket.exception.BusinessException;
import com.scenicticket.exception.DBException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiFormattersTest {
    @Test
    void formatsStatusesRolesMoneyAndDatesForUsers() {
        assertEquals("普通用户", UiFormatters.role("USER"));
        assertEquals("管理员", UiFormatters.role("ADMIN"));
        assertEquals("上架", UiFormatters.itemStatus(1));
        assertEquals("已支付", UiFormatters.orderStatus(1));
        assertEquals("无优惠", UiFormatters.discount(BigDecimal.ZERO));
        assertEquals("减免20%", UiFormatters.discount(new BigDecimal("20.00")));
        assertEquals("¥88.00", UiFormatters.money(new BigDecimal("88")));
        assertEquals("2026-07-10 08:30:00", UiFormatters.date(LocalDateTime.of(2026, 7, 10, 8, 30)));
    }

    @Test
    void calculatesDisplayedOrderAmountUsingPriceDiscountAndQuantity() {
        assertEquals(new BigDecimal("216.00"), UiFormatters.orderAmount(
                new BigDecimal("90.00"), new BigDecimal("20"), 3));
        assertEquals(new BigDecimal("72.00"), UiFormatters.discountedUnitPrice(
                new BigDecimal("90.00"), new BigDecimal("20")));
    }

    @Test
    void hidesTechnicalErrorsButKeepsChineseValidationMessages() {
        assertEquals("手机号格式不正确", UiFormatters.chineseError(new BusinessException("手机号格式不正确")));
        assertEquals("数据库操作失败，请检查数据库服务或稍后重试",
                UiFormatters.chineseError(new DBException("connection refused")));
        assertEquals("权限不足，无法完成该操作",
                UiFormatters.chineseError(new SecurityException("denied")));
        assertEquals("操作未完成，请稍后重试",
                UiFormatters.chineseError(new RuntimeException("unknown")));
        assertEquals("历史数据编码异常，暂无法显示",
                UiFormatters.readableText("????????", "暂无内容"));
        assertEquals("暂无内容", UiFormatters.readableText(" ", "暂无内容"));
    }

    @Test
    void explainsWhenTheBusinessDatabaseSchemaWasNotUpgraded() {
        SQLException missingColumn = new SQLException("Unknown column 'ticket_type_id'", "42S22", 1054);
        assertEquals("数据库结构未升级，请联系管理员执行数据库迁移",
                UiFormatters.chineseError(new DBException("Failed to search orders.", missingColumn)));

        SQLException missingTable = new SQLException("Table 'ticket_types' doesn't exist", "42S02", 1146);
        assertEquals("数据库结构未升级，请联系管理员执行数据库迁移",
                UiFormatters.chineseError(new DBException("Failed to load ticket types.", missingTable)));
    }
}
