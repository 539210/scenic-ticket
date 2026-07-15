package com.scenicticket.ui;

import com.scenicticket.dto.HotItemRankingDTO;
import com.scenicticket.dto.MonthlyOrderDetailDTO;
import com.scenicticket.dto.MonthlyOrderReportDTO;
import com.scenicticket.dto.StatisticsReportDTO;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportPanelTest {
    @Test
    void hotRankingDisplaysScenicNameAndRefreshKeepsCurrentReport() {
        FakeActions actions = new FakeActions();
        actions.hotItems = List.of(ranking(7L, "星湖湿地", 1, true));
        ReportPanel panel = new ReportPanel(2L, false, new ImmediateTaskExecutor(), actions);

        assertTrue(panel.reportButtonSelected("月度订单"));

        click(panel, "热门排行");
        assertTrue(panel.reportButtonSelected("热门排行"));
        assertTrue(!panel.reportButtonSelected("月度订单"));
        click(panel, "刷新当前报表");

        assertEquals(2, actions.hotCalls);
        assertEquals(1, panel.selectedTab());
        assertEquals("星湖湿地", panel.tableValueAt(1, 0, 1));
        assertEquals(7L, panel.tableValueAt(1, 0, 2));
        assertEquals("上架", panel.tableValueAt(1, 0, 3));
        assertTrue(panel.reportButtonSelected("热门排行"));
    }

    @Test
    void emptyMonthlyReportHasExplicitStatusAndParsedPeriod() {
        FakeActions actions = new FakeActions();
        ReportPanel panel = new ReportPanel(2L, false, new ImmediateTaskExecutor(), actions);
        panel.setYearMonth("2026", "6");

        click(panel, "月度订单");

        assertEquals(2026, actions.year);
        assertEquals(6, actions.month);
        assertEquals(0, panel.rowCount(0));
        assertTrue(actions.statuses.contains("该月份暂无订单数据"));
    }

    @Test
    void userReportTargetsSelfForUserAndRequestedUserForAdmin() {
        FakeActions userActions = new FakeActions();
        ReportPanel userPanel = new ReportPanel(2L, false, new ImmediateTaskExecutor(), userActions);
        userPanel.setTargetUserId("99");
        click(userPanel, "我的报告");

        FakeActions adminActions = new FakeActions();
        ReportPanel adminPanel = new ReportPanel(1L, true, new ImmediateTaskExecutor(), adminActions);
        adminPanel.setTargetUserId("99");
        click(adminPanel, "用户报告");

        assertEquals(2L, userActions.targetUserId);
        assertEquals(99L, adminActions.targetUserId);
        assertTrue(userActions.statuses.contains("该用户暂无行为数据"));
    }

    @Test
    void adminCanOpenAllEffectiveOrdersForSelectedMonthlyRow() {
        FakeActions actions = new FakeActions();
        LocalDate orderDate = LocalDate.of(2026, 7, 1);
        MonthlyOrderReportDTO summary = new MonthlyOrderReportDTO();
        summary.setOrderDate(orderDate);
        summary.setOrderCount(1);
        summary.setTotalAmount(new BigDecimal("88.00"));
        actions.monthlyReports = List.of(summary);
        actions.monthlyDetails = List.of(new MonthlyOrderDetailDTO(
                31L, 2L, "demo_user", 10L, "南山日出观景区", "成人票", 1,
                new BigDecimal("88.00"), new BigDecimal("88.00"), "微信", 1,
                LocalDate.of(2026, 7, 5), LocalDateTime.of(2026, 7, 1, 10, 30)));
        ReportPanel panel = new ReportPanel(1L, true, new ImmediateTaskExecutor(), actions);

        click(panel, "月度订单");
        panel.openMonthlyDetailsAt(0);

        assertEquals(orderDate, actions.requestedDetailDate);
        assertEquals(orderDate, actions.shownDetailDate);
        assertEquals(1, actions.shownDetails.size());
        assertEquals("南山日出观景区", actions.shownDetails.getFirst().itemTitle());
        assertTrue(actions.statuses.contains("已加载 2026-07-01 的 1 笔订单明细"));
    }

    private static HotItemRankingDTO ranking(long itemId, String title, int status, boolean found) {
        HotItemRankingDTO ranking = new HotItemRankingDTO();
        ranking.setItemId(itemId);
        ranking.setItemTitle(title);
        ranking.setItemStatus(status);
        ranking.setItemFound(found);
        ranking.setTotalActions(12);
        ranking.setViewCount(8);
        ranking.setOrderCount(3);
        ranking.setAvgDuration(45.5);
        return ranking;
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

    private static final class FakeActions implements ReportPanel.Actions {
        private List<HotItemRankingDTO> hotItems = List.of();
        private List<MonthlyOrderReportDTO> monthlyReports = List.of();
        private List<MonthlyOrderDetailDTO> monthlyDetails = List.of();
        private List<MonthlyOrderDetailDTO> shownDetails = List.of();
        private final List<String> statuses = new ArrayList<>();
        private int hotCalls;
        private int year;
        private int month;
        private long targetUserId;
        private LocalDate requestedDetailDate;
        private LocalDate shownDetailDate;

        @Override
        public List<MonthlyOrderReportDTO> monthly(int year, int month) {
            this.year = year;
            this.month = month;
            return monthlyReports;
        }

        @Override
        public List<MonthlyOrderDetailDTO> monthlyDetails(LocalDate orderDate) {
            requestedDetailDate = orderDate;
            return monthlyDetails;
        }

        @Override
        public void showMonthlyDetails(LocalDate orderDate, List<MonthlyOrderDetailDTO> details) {
            shownDetailDate = orderDate;
            shownDetails = details;
        }

        @Override
        public List<HotItemRankingDTO> hot() {
            hotCalls++;
            return hotItems;
        }

        @Override
        public Document userReport(long targetUserId) {
            this.targetUserId = targetUserId;
            return new Document();
        }

        @Override
        public StatisticsReportDTO dashboard(int year, int month) {
            return new StatisticsReportDTO();
        }

        @Override
        public void setStatus(String message) {
            statuses.add(message);
        }
    }
}
