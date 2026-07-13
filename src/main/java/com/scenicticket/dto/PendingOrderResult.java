package com.scenicticket.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PendingOrderResult(long orderId, BigDecimal totalAmount, String ticketTypeName,
                                 LocalDate visitDate, int quantity, boolean auditRecorded,
                                 String message) {
}
