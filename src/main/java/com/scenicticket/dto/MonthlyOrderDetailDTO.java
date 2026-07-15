package com.scenicticket.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MonthlyOrderDetailDTO(
        long orderId,
        long userId,
        String username,
        long itemId,
        String itemTitle,
        String ticketTypeName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        String paymentMethod,
        int status,
        LocalDate visitDate,
        LocalDateTime createdAt) {
}
