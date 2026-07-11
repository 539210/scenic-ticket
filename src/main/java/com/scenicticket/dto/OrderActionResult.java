package com.scenicticket.dto;

public record OrderActionResult(long orderId, boolean updated, boolean auditRecorded, String message) {
}
