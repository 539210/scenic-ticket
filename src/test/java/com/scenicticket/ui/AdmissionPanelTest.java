package com.scenicticket.ui;

import com.scenicticket.dto.AdmissionResult;
import com.scenicticket.model.Admission;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdmissionPanelTest {
    @Test
    void queryRendersQuantityOperatorTimeAndBlankNote() {
        FakeActions actions = new FakeActions();
        actions.admissions = List.of(admission(9L, 31L, 2, 1L, ""));
        AdmissionPanel panel = new AdmissionPanel(new ImmediateTaskExecutor(), actions);
        panel.setForm("31", 1, "");

        click(panel, "查询核销记录");

        assertEquals(31L, actions.lastListedOrderId);
        assertEquals(1, panel.rowCount());
        assertEquals(2, panel.tableValueAt(0, 2));
        assertEquals(1L, panel.tableValueAt(0, 3));
        assertEquals("-", panel.tableValueAt(0, 5));
        assertTrue(actions.statuses.contains("已加载 1 条核销记录"));
    }

    @Test
    void admitPassesFrozenFormValuesAndRefreshesSameOrder() {
        FakeActions actions = new FakeActions();
        AdmissionPanel panel = new AdmissionPanel(new ImmediateTaskExecutor(), actions);
        panel.setForm("42", 3, "团队入园");

        click(panel, "确认核销");

        assertEquals(42L, actions.admittedOrderId);
        assertEquals(3, actions.admittedQuantity);
        assertEquals("团队入园", actions.admittedNote);
        assertEquals(42L, actions.lastListedOrderId);
        assertEquals("核销成功", actions.statuses.get(actions.statuses.size() - 1));
    }

    @Test
    void emptyQueryHasExplicitOrderSpecificStatus() {
        FakeActions actions = new FakeActions();
        AdmissionPanel panel = new AdmissionPanel(new ImmediateTaskExecutor(), actions);
        panel.setForm("77", 1, "");

        click(panel, "查询核销记录");

        assertEquals(0, panel.rowCount());
        assertTrue(actions.statuses.contains("订单 77 暂无核销记录"));
    }

    private static Admission admission(long admissionId, long orderId, int quantity,
                                       long operatorId, String note) {
        Admission admission = new Admission();
        admission.setAdmissionId(admissionId);
        admission.setOrderId(orderId);
        admission.setQuantity(quantity);
        admission.setOperatorUserId(operatorId);
        admission.setAdmittedAt(LocalDateTime.of(2026, 7, 13, 10, 30));
        admission.setNote(note);
        return admission;
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

    private static final class FakeActions implements AdmissionPanel.Actions {
        private List<Admission> admissions = List.of();
        private final List<String> statuses = new ArrayList<>();
        private long lastListedOrderId;
        private long admittedOrderId;
        private int admittedQuantity;
        private String admittedNote;

        @Override
        public List<Admission> listByOrder(long orderId) {
            lastListedOrderId = orderId;
            return admissions;
        }

        @Override
        public AdmissionResult admit(long orderId, int quantity, String note) {
            admittedOrderId = orderId;
            admittedQuantity = quantity;
            admittedNote = note;
            return new AdmissionResult(orderId, quantity, 0, true, true, "核销成功");
        }

        @Override
        public void setStatus(String message) {
            statuses.add(message);
        }
    }
}
