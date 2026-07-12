package com.scenicticket.ui;

import com.scenicticket.model.Order;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderActionPolicyTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 12);

    @Test
    void pendingOwnerCanPayOrCancelButCannotRefund() {
        OrderActionPolicy.Availability availability = OrderActionPolicy.evaluate(2L,
                order(2L, 0, TODAY.plusDays(1)), TODAY);

        assertTrue(availability.canPay());
        assertTrue(availability.canCancel());
        assertFalse(availability.canRefund());
    }

    @Test
    void paidOwnerCanRefundOnlyBeforeVisitDate() {
        OrderActionPolicy.Availability future = OrderActionPolicy.evaluate(2L,
                order(2L, 1, TODAY.plusDays(1)), TODAY);
        OrderActionPolicy.Availability today = OrderActionPolicy.evaluate(2L,
                order(2L, 1, TODAY), TODAY);

        assertTrue(future.canRefund());
        assertFalse(future.canPay());
        assertFalse(future.canCancel());
        assertFalse(today.canRefund());
    }

    @Test
    void otherUsersAndTerminalOrdersExposeNoLifecycleActions() {
        OrderActionPolicy.Availability otherUser = OrderActionPolicy.evaluate(1L,
                order(2L, 0, TODAY.plusDays(1)), TODAY);
        OrderActionPolicy.Availability cancelled = OrderActionPolicy.evaluate(2L,
                order(2L, 2, TODAY.plusDays(1)), TODAY);
        OrderActionPolicy.Availability completed = OrderActionPolicy.evaluate(2L,
                order(2L, 3, TODAY.plusDays(1)), TODAY);

        assertFalse(otherUser.canPay() || otherUser.canCancel() || otherUser.canRefund());
        assertFalse(cancelled.canPay() || cancelled.canCancel() || cancelled.canRefund());
        assertFalse(completed.canPay() || completed.canCancel() || completed.canRefund());
    }

    private static Order order(long userId, int status, LocalDate visitDate) {
        Order order = new Order();
        order.setOrderId(10L);
        order.setUserId(userId);
        order.setStatus(status);
        order.setVisitDate(visitDate);
        return order;
    }
}
