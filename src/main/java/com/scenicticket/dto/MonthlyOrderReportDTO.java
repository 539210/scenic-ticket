package com.scenicticket.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MonthlyOrderReportDTO {
    private LocalDate orderDate;
    private int orderCount;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }
}
