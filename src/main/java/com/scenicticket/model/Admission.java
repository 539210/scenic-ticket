package com.scenicticket.model;

import java.time.LocalDateTime;

public class Admission {
    private Long admissionId;
    private Long orderId;
    private Integer quantity;
    private Long operatorUserId;
    private LocalDateTime admittedAt;
    private String note;

    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getOperatorUserId() { return operatorUserId; }
    public void setOperatorUserId(Long operatorUserId) { this.operatorUserId = operatorUserId; }
    public LocalDateTime getAdmittedAt() { return admittedAt; }
    public void setAdmittedAt(LocalDateTime admittedAt) { this.admittedAt = admittedAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
