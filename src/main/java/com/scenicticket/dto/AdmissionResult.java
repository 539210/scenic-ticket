package com.scenicticket.dto;

public record AdmissionResult(long orderId, int admittedQuantity, int remainingQuantity,
                              boolean completed, boolean auditRecorded, String message) {
}
