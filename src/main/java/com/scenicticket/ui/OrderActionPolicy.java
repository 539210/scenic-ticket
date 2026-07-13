package com.scenicticket.ui;

import com.scenicticket.model.Order;

import java.time.LocalDate;

public final class OrderActionPolicy {
    private OrderActionPolicy() {
    }

    public static Availability evaluate(long actorUserId, Order order, LocalDate today) {
        if (actorUserId <= 0 || order == null || order.getUserId() == null
                || order.getUserId() != actorUserId || order.getStatus() == null) {
            return Availability.NONE;
        }
        if (order.getStatus() == 0) {
            return new Availability(true, true, false);
        }
        boolean refundable = order.getStatus() == 1
                && order.getRefundedAt() == null
                && order.getVisitDate() != null
                && today != null
                && today.isBefore(order.getVisitDate());
        return refundable ? new Availability(false, false, true) : Availability.NONE;
    }

    public record Availability(boolean canPay, boolean canCancel, boolean canRefund) {
        static final Availability NONE = new Availability(false, false, false);
    }
}
