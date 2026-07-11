package com.scenicticket.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

public class Order {
    private Long orderId;
    private Long userId;
    private Long itemId;
    private BigDecimal amount;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountRate;
    private String paymentMethod;
    private Long ticketTypeId;
    private String ticketTypeNameSnapshot;
    private BigDecimal originalUnitPrice;
    private BigDecimal discountedUnitPrice;
    private LocalDate visitDate;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;
    private LocalDateTime refundedAt;
    private Integer statusVersion;
    private Integer status;
    private LocalDateTime createdAt;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getTicketTypeId() { return ticketTypeId; }
    public void setTicketTypeId(Long ticketTypeId) { this.ticketTypeId = ticketTypeId; }
    public String getTicketTypeNameSnapshot() { return ticketTypeNameSnapshot; }
    public void setTicketTypeNameSnapshot(String ticketTypeNameSnapshot) { this.ticketTypeNameSnapshot = ticketTypeNameSnapshot; }
    public BigDecimal getOriginalUnitPrice() { return originalUnitPrice; }
    public void setOriginalUnitPrice(BigDecimal originalUnitPrice) { this.originalUnitPrice = originalUnitPrice; }
    public BigDecimal getDiscountedUnitPrice() { return discountedUnitPrice; }
    public void setDiscountedUnitPrice(BigDecimal discountedUnitPrice) { this.discountedUnitPrice = discountedUnitPrice; }
    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
    public Integer getStatusVersion() { return statusVersion; }
    public void setStatusVersion(Integer statusVersion) { this.statusVersion = statusVersion; }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
