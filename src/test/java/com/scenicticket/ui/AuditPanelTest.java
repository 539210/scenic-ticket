package com.scenicticket.ui;

import com.scenicticket.dto.AuditLogQuery;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditPanelTest {
    @Test
    void combinedFiltersReachServiceAndRenderReadableLogRow() {
        FakeActions actions = new FakeActions();
        actions.logs = List.of(new Document("timestamp", dateAtStart("2026-07-13"))
                .append("user_id", 8L)
                .append("log_type", "ORDER_PAY")
                .append("log_level", "WARN")
                .append("message", "订单支付成功")
                .append("action_detail", new Document("operation", "支付购票").append("order_id", 88L)
                        .append("item_id", 7L).append("ticket_type_name", "学生票")
                        .append("visit_date", "2026-07-20").append("quantity", 2)
                        .append("amount", "160.00").append("payment_method", "微信")
                        .append("ip", "127.0.0.1")));
        AuditPanel panel = new AuditPanel(new ImmediateTaskExecutor(), actions);
        panel.setFilters("8", 5, 2, "2026-07-01", "2026-07-13", "订单88", "120");

        click(panel, "查询日志");

        AuditLogQuery query = actions.lastQuery;
        assertNotNull(query);
        assertEquals(8L, query.getUserId());
        assertEquals("ORDER_PAY", query.getLogType());
        assertEquals("WARN", query.getLogLevel());
        assertEquals("订单88", query.getKeyword());
        assertEquals(120, query.getLimit());
        assertEquals(dateAtStart("2026-07-01"), query.getStartTime());
        assertEquals(dateAtEnd("2026-07-13"), query.getEndTime());
        assertEquals(1, panel.rowCount(0));
        assertEquals("支付购票", panel.tableValueAt(0, 0, 2));
        assertEquals("警告", panel.tableValueAt(0, 0, 3));
        assertEquals("订单 #88 / 景点 #7", panel.tableValueAt(0, 0, 5));
        assertTrue(String.valueOf(panel.tableValueAt(0, 0, 6)).contains("票种=学生票"));
        assertEquals("127.0.0.1", panel.tableValueAt(0, 0, 7));
        assertTrue(actions.statuses.contains("查询到 1 条审计日志"));
    }

    @Test
    void clearResetsEveryFilterAndRunsUnfilteredQuery() {
        FakeActions actions = new FakeActions();
        AuditPanel panel = new AuditPanel(new ImmediateTaskExecutor(), actions);
        panel.setFilters("9", 6, 3, "2026-07-01", "2026-07-13", "报表", "20");

        click(panel, "清空条件");

        AuditLogQuery query = actions.lastQuery;
        assertNull(query.getUserId());
        assertNull(query.getLogType());
        assertNull(query.getLogLevel());
        assertNull(query.getStartTime());
        assertNull(query.getEndTime());
        assertEquals("", query.getKeyword());
        assertEquals(80, query.getLimit());
    }

    @Test
    void refreshRepeatsCurrentSummaryInsteadOfSwitchingToLogs() {
        FakeActions actions = new FakeActions();
        actions.summary = List.of(new Document("log_type", "ORDER_CREATE")
                .append("log_level", "INFO")
                .append("operation_count", 6)
                .append("user_count", 3));
        AuditPanel panel = new AuditPanel(new ImmediateTaskExecutor(), actions);

        click(panel, "审计汇总");
        click(panel, "刷新当前结果");

        assertEquals(2, actions.summaryCalls);
        assertEquals(1, panel.selectedTab());
        assertEquals(1, panel.rowCount(1));
        assertEquals("预定下单", panel.tableValueAt(1, 0, 0));
        assertEquals("正常", panel.tableValueAt(1, 0, 1));
        assertTrue(panel.actionSelected(1));
    }

    private static Date dateAtStart(String value) {
        return Date.from(LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date dateAtEnd(String value) {
        return Date.from(LocalDate.parse(value).plusDays(1).atStartOfDay(ZoneId.systemDefault())
                .minusNanos(1_000_000).toInstant());
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

    private static final class FakeActions implements AuditPanel.Actions {
        private List<Document> logs = List.of();
        private List<Document> summary = List.of();
        private final List<String> statuses = new ArrayList<>();
        private AuditLogQuery lastQuery;
        private int summaryCalls;

        @Override
        public List<Document> query(AuditLogQuery query) {
            lastQuery = query;
            return logs;
        }

        @Override
        public List<Document> summary(Date startTime, Date endTime) {
            summaryCalls++;
            return summary;
        }

        @Override
        public List<Document> trend(Date startTime, Date endTime) {
            return List.of();
        }

        @Override
        public List<Document> userSummary(Date startTime, Date endTime, int limit) {
            return List.of();
        }

        @Override
        public void setStatus(String message) {
            statuses.add(message);
        }
    }
}
