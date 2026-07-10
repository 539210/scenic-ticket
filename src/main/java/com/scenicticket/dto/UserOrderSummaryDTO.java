package com.scenicticket.dto;

import java.math.BigDecimal;

public class UserOrderSummaryDTO {
    private long totalOrders;
    private long pendingOrders;
    private long paidOrders;
    private long cancelledOrders;
    private long completedOrders;
    private BigDecimal paidAmount = BigDecimal.ZERO;

    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
    public long getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }
    public long getPaidOrders() { return paidOrders; }
    public void setPaidOrders(long paidOrders) { this.paidOrders = paidOrders; }
    public long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(long cancelledOrders) { this.cancelledOrders = cancelledOrders; }
    public long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(long completedOrders) { this.completedOrders = completedOrders; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount == null ? BigDecimal.ZERO : paidAmount; }
}
