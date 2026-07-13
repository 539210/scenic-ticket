package com.scenicticket.ui;

import com.scenicticket.dto.OrderActionResult;
import com.scenicticket.dto.OrderViewDTO;
import com.scenicticket.model.Order;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderPanelTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 13);

    @Test
    void userQueryIsBoundToCurrentUserAndPendingSelectionEnablesLegalActions() {
        FakeActions actions = new FakeActions(List.of(view(31L, 2L, 0, TODAY.plusDays(2))));
        OrderPanel panel = new OrderPanel(2L, false, new ImmediateTaskExecutor(), actions, () -> TODAY);

        click(panel, "查询订单");
        panel.selectRow(0);

        assertEquals(2L, actions.lastQueryUserId);
        assertEquals(1, panel.rowCount());
        assertTrue(panel.payEnabled());
        assertTrue(panel.cancelEnabled());
        assertFalse(panel.refundEnabled());

        click(panel, "确认支付");
        assertEquals(List.of(31L), actions.paidOrderIds);
        assertTrue(actions.statuses.contains("支付成功"));
        assertFalse(panel.payEnabled());
    }

    @Test
    void adminCannotOperateAnotherUsersOrderFromSearchResults() {
        FakeActions actions = new FakeActions(List.of(view(42L, 2L, 0, TODAY.plusDays(1))));
        OrderPanel panel = new OrderPanel(1L, true, new ImmediateTaskExecutor(), actions, () -> TODAY);

        click(panel, "查询订单");
        panel.selectRow(0);

        assertNull(actions.lastQueryUserId);
        assertFalse(panel.payEnabled());
        assertFalse(panel.cancelEnabled());
        assertFalse(panel.refundEnabled());
    }

    @Test
    void paidFutureOrderRequestsReasonBeforeRefund() {
        FakeActions actions = new FakeActions(List.of(view(53L, 2L, 1, TODAY.plusDays(3))));
        actions.refundReason = "行程变更";
        OrderPanel panel = new OrderPanel(2L, false, new ImmediateTaskExecutor(), actions, () -> TODAY);

        click(panel, "查询订单");
        panel.selectRow(0);
        assertTrue(panel.refundEnabled());
        click(panel, "申请模拟退款");

        assertEquals(53L, actions.refundedOrderId);
        assertEquals("行程变更", actions.capturedRefundReason);
    }

    @Test
    void lifecycleActionKeepsOrderSelectedAtClickTime() {
        FakeActions actions = new FakeActions(List.of(
                view(61L, 2L, 0, TODAY.plusDays(1)),
                view(62L, 2L, 0, TODAY.plusDays(1))));
        DeferredLifecycleExecutor executor = new DeferredLifecycleExecutor();
        OrderPanel panel = new OrderPanel(2L, false, executor, actions, () -> TODAY);

        click(panel, "查询订单");
        panel.selectRow(0);
        click(panel, "确认支付");
        panel.selectRow(1);
        executor.complete();

        assertEquals(List.of(61L), actions.paidOrderIds);
    }

    private static OrderViewDTO view(long orderId, long userId, int status, LocalDate visitDate) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setStatus(status);
        order.setVisitDate(visitDate);
        order.setQuantity(1);
        OrderViewDTO view = new OrderViewDTO();
        view.setOrder(order);
        view.setItemTitle("测试景点");
        return view;
    }

    private static void click(Container root, String text) {
        JButton button = findButton(root, text);
        assertNotNull(button);
        button.doClick();
    }

    private static JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container child) {
                JButton found = findButton(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static final class ImmediateTaskExecutor implements UiTaskExecutor {
        @Override
        public <T> void run(String name, Callable<T> task, Consumer<T> onSuccess) {
            try {
                onSuccess.accept(task.call());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private static final class DeferredLifecycleExecutor implements UiTaskExecutor {
        private Runnable deferred;

        @Override
        public <T> void run(String name, Callable<T> task, Consumer<T> onSuccess) {
            if ("确认支付".equals(name)) {
                deferred = () -> execute(task, onSuccess);
                return;
            }
            execute(task, onSuccess);
        }

        private <T> void execute(Callable<T> task, Consumer<T> onSuccess) {
            try {
                onSuccess.accept(task.call());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        private void complete() {
            assertNotNull(deferred);
            deferred.run();
        }
    }

    private static final class FakeActions implements OrderPanel.Actions {
        private final List<OrderViewDTO> views;
        private final List<Long> paidOrderIds = new ArrayList<>();
        private final List<String> statuses = new ArrayList<>();
        private Long lastQueryUserId;
        private Long refundedOrderId;
        private String refundReason;
        private String capturedRefundReason;

        private FakeActions(List<OrderViewDTO> views) {
            this.views = views;
        }

        @Override
        public List<OrderViewDTO> search(Long queryUserId, Long orderId, Integer status) {
            lastQueryUserId = queryUserId;
            return views;
        }

        @Override
        public OrderActionResult pay(long orderId) {
            paidOrderIds.add(orderId);
            return new OrderActionResult(orderId, true, true, "支付成功");
        }

        @Override
        public OrderActionResult cancelPending(long orderId) {
            return new OrderActionResult(orderId, true, true, "取消成功");
        }

        @Override
        public OrderActionResult refund(long orderId, String reason) {
            refundedOrderId = orderId;
            capturedRefundReason = reason;
            return new OrderActionResult(orderId, true, true, "退款成功");
        }

        @Override
        public String requestRefundReason() {
            return refundReason;
        }

        @Override
        public void setStatus(String message) {
            statuses.add(message);
        }
    }
}
