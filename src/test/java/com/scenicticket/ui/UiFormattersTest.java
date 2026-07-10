package com.scenicticket.ui;

import com.scenicticket.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiFormattersTest {
    @Test
    void formatsStatusesRolesMoneyAndDatesForUsers() {
        assertEquals("普通用户", UiFormatters.role("USER"));
        assertEquals("管理员", UiFormatters.role("ADMIN"));
        assertEquals("上架", UiFormatters.itemStatus(1));
        assertEquals("已支付", UiFormatters.orderStatus(1));
        assertEquals("无折扣", UiFormatters.discount(BigDecimal.ZERO));
        assertEquals("20%", UiFormatters.discount(new BigDecimal("20.00")));
        assertEquals("¥88.00", UiFormatters.money(new BigDecimal("88")));
        assertEquals("2026-07-10 08:30:00", UiFormatters.date(LocalDateTime.of(2026, 7, 10, 8, 30)));
    }

    @Test
    void calculatesDisplayedOrderAmountUsingPriceDiscountAndQuantity() {
        assertEquals(new BigDecimal("216.00"), UiFormatters.orderAmount(
                new BigDecimal("90.00"), new BigDecimal("20"), 3));
    }

    @Test
    void hidesTechnicalErrorsButKeepsChineseValidationMessages() {
        assertEquals("手机号格式不正确", UiFormatters.chineseError(new BusinessException("手机号格式不正确")));
        assertEquals("操作未完成，请检查数据库连接或稍后重试",
                UiFormatters.chineseError(new RuntimeException("connection refused")));
    }
}
