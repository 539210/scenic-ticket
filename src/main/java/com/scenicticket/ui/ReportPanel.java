package com.scenicticket.ui;

import com.scenicticket.dto.HotItemRankingDTO;
import com.scenicticket.dto.MonthlyOrderDetailDTO;
import com.scenicticket.dto.MonthlyOrderReportDTO;
import com.scenicticket.dto.StatisticsReportDTO;
import org.bson.Document;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;

public final class ReportPanel extends JPanel {
    private final long actorUserId;
    private final boolean admin;
    private final UiTaskExecutor taskExecutor;
    private final Actions actions;
    private final JTextField yearField = new JTextField(String.valueOf(LocalDate.now().getYear()), 6);
    private final JTextField monthField = new JTextField(String.valueOf(LocalDate.now().getMonthValue()), 4);
    private final JTextField userIdField = new JTextField(10);
    private final DefaultTableModel monthlyModel = tableModel("日期", "订单数", "销售金额");
    private final JTable monthlyTable = UiComponents.table(monthlyModel);
    private final DefaultTableModel hotModel = tableModel(
            "排名", "景点名称", "景点ID", "状态", "总操作", "浏览", "下单", "平均停留(秒)");
    private final DefaultTableModel userModel = tableModel("指标", "数据");
    private final DefaultTableModel dashboardModel = tableModel("模块", "指标", "数据");
    private final JTabbedPane resultTabs = new JTabbedPane(JTabbedPane.TOP);
    private JButton monthlyButton;
    private JButton hotButton;
    private JButton userButton;
    private JButton dashboardButton;

    public ReportPanel(long actorUserId, boolean admin, UiTaskExecutor taskExecutor, Actions actions) {
        super(new BorderLayout(12, 12));
        if (actorUserId <= 0) {
            throw new IllegalArgumentException("当前用户无效，请重新登录");
        }
        this.actorUserId = actorUserId;
        this.admin = admin;
        this.taskExecutor = taskExecutor;
        this.actions = actions;
        setBackground(UiTheme.BACKGROUND);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        configureMonthlyTable();
        resultTabs.addTab("月度订单", createMonthlyResultPanel());
        resultTabs.addTab("热门排行", UiComponents.scroll(UiComponents.table(hotModel)));
        resultTabs.addTab(admin ? "用户报告" : "我的报告", UiComponents.scroll(UiComponents.table(userModel)));
        if (admin) {
            resultTabs.addTab("综合汇总", UiComponents.scroll(UiComponents.table(dashboardModel)));
        }
        add(UiComponents.card("报表条件", createToolbar()), BorderLayout.NORTH);
        add(UiComponents.card("报表数据", resultTabs), BorderLayout.CENTER);
        resultTabs.addChangeListener(event -> applyReportButtonStyles(resultTabs.getSelectedIndex()));
        setActiveReport(0);
    }

    private void configureMonthlyTable() {
        if (!admin) {
            return;
        }
        monthlyTable.setToolTipText("单击日期行查看当天订单明细");
        monthlyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                int viewRow = monthlyTable.rowAtPoint(event.getPoint());
                if (viewRow >= 0) {
                    openMonthlyDetailsAt(monthlyTable.convertRowIndexToModel(viewRow));
                }
            }
        });
    }

    private JPanel createMonthlyResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        if (admin) {
            JLabel hint = new JLabel("单击日期行可查看当天每笔订单的购买用户和景点明细");
            hint.setForeground(UiTheme.MUTED);
            panel.add(hint, BorderLayout.NORTH);
        }
        panel.add(UiComponents.scroll(monthlyTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createToolbar() {
        JPanel filters = UiComponents.toolbar();
        JPanel buttons = UiComponents.toolbar();
        JPanel toolbar = new JPanel(new GridLayout(2, 1, 0, 4));
        toolbar.setOpaque(false);
        monthlyButton = UiComponents.secondaryButton("月度订单");
        hotButton = UiComponents.secondaryButton("热门排行");
        userButton = UiComponents.secondaryButton(admin ? "用户报告" : "我的报告");
        dashboardButton = UiComponents.secondaryButton("综合汇总");
        JButton refreshButton = UiComponents.secondaryButton("刷新当前报表");

        filters.add(new JLabel("年份"));
        filters.add(yearField);
        filters.add(new JLabel("月份"));
        filters.add(monthField);
        if (admin) {
            filters.add(new JLabel("用户ID"));
            filters.add(userIdField);
        }
        buttons.add(monthlyButton);
        buttons.add(hotButton);
        buttons.add(userButton);
        if (admin) {
            buttons.add(dashboardButton);
        }
        buttons.add(refreshButton);
        toolbar.add(filters);
        toolbar.add(buttons);

        monthlyButton.addActionListener(event -> loadMonthly());
        hotButton.addActionListener(event -> loadHot());
        userButton.addActionListener(event -> loadUserReport());
        dashboardButton.addActionListener(event -> loadDashboard());
        refreshButton.addActionListener(event -> refreshCurrentReport());
        yearField.addActionListener(event -> loadMonthly());
        monthField.addActionListener(event -> loadMonthly());
        if (admin) {
            userIdField.addActionListener(event -> loadUserReport());
        }
        return toolbar;
    }

    private void loadMonthly() {
        setActiveReport(0);
        YearMonthSnapshot snapshot = yearMonthSnapshot();
        taskExecutor.run("月度订单报表", () -> actions.monthly(snapshot.year(), snapshot.month()), reports -> {
            List<MonthlyOrderReportDTO> safeReports = reports == null ? List.of() : reports;
            monthlyModel.setRowCount(0);
            for (MonthlyOrderReportDTO report : safeReports) {
                monthlyModel.addRow(new Object[]{report.getOrderDate(), report.getOrderCount(),
                        UiFormatters.money(report.getTotalAmount())});
            }
            actions.setStatus(safeReports.isEmpty() ? "该月份暂无订单数据" : "月度订单报表已更新");
        });
    }

    void openMonthlyDetailsAt(int modelRow) {
        if (!admin || modelRow < 0 || modelRow >= monthlyModel.getRowCount()) {
            return;
        }
        Object dateValue = monthlyModel.getValueAt(modelRow, 0);
        LocalDate orderDate = dateValue instanceof LocalDate localDate
                ? localDate : LocalDate.parse(String.valueOf(dateValue));
        taskExecutor.run("查询当日订单明细", () -> actions.monthlyDetails(orderDate), details -> {
            List<MonthlyOrderDetailDTO> safeDetails = details == null ? List.of() : details;
            if (safeDetails.isEmpty()) {
                actions.setStatus(orderDate + " 暂无有效订单明细");
                return;
            }
            actions.showMonthlyDetails(orderDate, safeDetails);
            actions.setStatus("已加载 " + orderDate + " 的 " + safeDetails.size() + " 笔订单明细");
        });
    }

    private void loadHot() {
        setActiveReport(1);
        taskExecutor.run("热门排行", actions::hot, rankings -> {
            List<HotItemRankingDTO> safeRankings = rankings == null ? List.of() : rankings;
            hotModel.setRowCount(0);
            int rank = 1;
            for (HotItemRankingDTO ranking : safeRankings) {
                hotModel.addRow(new Object[]{rank++, valueText(ranking.getItemTitle()), ranking.getItemId(),
                        ranking.isItemFound() ? UiFormatters.itemStatus(ranking.getItemStatus()) : "-",
                        ranking.getTotalActions(), ranking.getViewCount(), ranking.getOrderCount(),
                        decimalText(ranking.getAvgDuration())});
            }
            actions.setStatus(safeRankings.isEmpty() ? "暂无热门排行数据" : "热门排行已更新");
        });
    }

    private void loadUserReport() {
        setActiveReport(2);
        String userIdText = userIdField.getText();
        taskExecutor.run("用户报告", () -> {
            long targetUserId = admin && userIdText != null && !userIdText.isBlank()
                    ? UiInputParsers.requiredLong(userIdText, "用户ID") : actorUserId;
            return actions.userReport(targetUserId);
        }, document -> {
            fillUserReport(document);
            actions.setStatus(document == null || document.isEmpty() ? "该用户暂无行为数据" : "用户报告已更新");
        });
    }

    private void loadDashboard() {
        if (!admin) {
            return;
        }
        setActiveReport(3);
        YearMonthSnapshot snapshot = yearMonthSnapshot();
        taskExecutor.run("综合汇总", () -> actions.dashboard(snapshot.year(), snapshot.month()), dto -> {
            fillDashboard(dto);
            actions.setStatus(dashboardModel.getRowCount() == 0 ? "综合汇总暂无数据" : "综合汇总已更新");
        });
    }

    private void refreshCurrentReport() {
        switch (resultTabs.getSelectedIndex()) {
            case 0 -> loadMonthly();
            case 1 -> loadHot();
            case 2 -> loadUserReport();
            case 3 -> loadDashboard();
            default -> loadMonthly();
        }
    }

    private void setActiveReport(int index) {
        if (index < 0 || index >= resultTabs.getTabCount()) {
            return;
        }
        resultTabs.setSelectedIndex(index);
        applyReportButtonStyles(index);
    }

    private void applyReportButtonStyles(int index) {
        UiComponents.setSelectedStyle(monthlyButton, index == 0);
        UiComponents.setSelectedStyle(hotButton, index == 1);
        UiComponents.setSelectedStyle(userButton, index == 2);
        UiComponents.setSelectedStyle(dashboardButton, admin && index == 3);
    }

    private YearMonthSnapshot yearMonthSnapshot() {
        return new YearMonthSnapshot(yearField.getText(), monthField.getText());
    }

    private void fillUserReport(Document document) {
        userModel.setRowCount(0);
        if (document == null || document.isEmpty()) {
            return;
        }
        addMetric("用户ID", valueText(document.get("user_id")));
        addMetric("总操作次数", numberText(document.get("action_count")));
        addMetric("访问景点数", numberText(document.get("visited_item_count")));
        addMetric("浏览次数", numberText(document.get("view_count")));
        addMetric("搜索次数", numberText(document.get("search_count")));
        addMetric("评论次数", numberText(document.get("comment_count")));
        addMetric("下单次数", numberText(document.get("order_count")));
        addMetric("总停留时长", numberText(document.get("total_duration")) + " 秒");
        addMetric("平均停留时长", decimalText(document.get("avg_duration")) + " 秒");
        addMetric("首次操作", UiFormatters.date(document.get("first_action_time")));
        addMetric("最近操作", UiFormatters.date(document.get("latest_action_time")));
    }

    private void addMetric(String name, Object value) {
        userModel.addRow(new Object[]{name, value});
    }

    private void fillDashboard(StatisticsReportDTO dto) {
        dashboardModel.setRowCount(0);
        if (dto == null) {
            return;
        }
        for (HotItemRankingDTO ranking : safeList(dto.getHotItems())) {
            dashboardModel.addRow(new Object[]{"热门景点", valueText(ranking.getItemTitle()) + " / ID " + ranking.getItemId(),
                    "总操作 " + ranking.getTotalActions() + "，浏览 " + ranking.getViewCount()
                            + "，下单 " + ranking.getOrderCount()});
        }
        for (Document document : safeList(dto.getActionTypeSummary())) {
            dashboardModel.addRow(new Object[]{"用户行为", actionTypeName(document.getString("action_type")),
                    numberText(document.get("action_count")) + " 次"});
        }
        for (Document document : safeList(dto.getHotTags())) {
            dashboardModel.addRow(new Object[]{"热门标签", valueText(document.get("_id")),
                    numberText(document.get("tag_count")) + " 次"});
        }
        for (Document document : safeList(dto.getSystemAuditSummary())) {
            dashboardModel.addRow(new Object[]{"系统审计", logTypeName(document.getString("log_type")) + " / "
                    + logLevelName(document.getString("log_level")),
                    numberText(document.get("operation_count")) + " 次"});
        }
        for (MonthlyOrderReportDTO report : safeList(dto.getMonthlyOrderReport())) {
            dashboardModel.addRow(new Object[]{"月度订单", report.getOrderDate(),
                    report.getOrderCount() + " 单，" + UiFormatters.money(report.getTotalAmount())});
        }
    }

    private static String actionTypeName(String actionType) {
        return switch (actionType == null ? "" : actionType) {
            case "VIEW" -> "浏览景点";
            case "SEARCH" -> "搜索";
            case "ORDER" -> "下单";
            case "COMMENT" -> "评论";
            case "" -> "-";
            default -> actionType;
        };
    }

    private static String logTypeName(String logType) {
        return switch (logType == null ? "" : logType) {
            case "LOGIN" -> "登录";
            case "LOGOUT" -> "退出";
            case "REGISTER" -> "注册";
            case "ORDER_CREATE", "ORDER" -> "创建订单";
            case "ITEM_UPDATE" -> "景点更新";
            case "REPORT_VIEW" -> "查看报表";
            case "" -> "-";
            default -> logType;
        };
    }

    private static String logLevelName(String level) {
        return switch (level == null ? "" : level) {
            case "INFO" -> "正常";
            case "WARN" -> "警告";
            case "ERROR" -> "错误";
            case "" -> "-";
            default -> level;
        };
    }

    private static String valueText(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static String numberText(Object value) {
        return value instanceof Number number ? String.valueOf(number.longValue()) : valueText(value);
    }

    private static String decimalText(Object value) {
        return value instanceof Number number ? String.format("%.2f", number.doubleValue()) : valueText(value);
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    void setYearMonth(String year, String month) {
        yearField.setText(year);
        monthField.setText(month);
    }

    void setTargetUserId(String userId) {
        userIdField.setText(userId);
    }

    int selectedTab() {
        return resultTabs.getSelectedIndex();
    }

    boolean reportButtonSelected(String text) {
        JButton button = switch (text) {
            case "月度订单" -> monthlyButton;
            case "热门排行" -> hotButton;
            case "用户报告", "我的报告" -> userButton;
            case "综合汇总" -> dashboardButton;
            default -> null;
        };
        return UiComponents.isSelectedStyle(button);
    }

    int rowCount(int tabIndex) {
        return switch (tabIndex) {
            case 0 -> monthlyModel.getRowCount();
            case 1 -> hotModel.getRowCount();
            case 2 -> userModel.getRowCount();
            case 3 -> dashboardModel.getRowCount();
            default -> throw new IllegalArgumentException("未知报表页签");
        };
    }

    Object tableValueAt(int tabIndex, int row, int column) {
        return switch (tabIndex) {
            case 0 -> monthlyModel.getValueAt(row, column);
            case 1 -> hotModel.getValueAt(row, column);
            case 2 -> userModel.getValueAt(row, column);
            case 3 -> dashboardModel.getValueAt(row, column);
            default -> throw new IllegalArgumentException("未知报表页签");
        };
    }

    public interface Actions {
        List<MonthlyOrderReportDTO> monthly(int year, int month);

        List<MonthlyOrderDetailDTO> monthlyDetails(LocalDate orderDate);

        void showMonthlyDetails(LocalDate orderDate, List<MonthlyOrderDetailDTO> details);

        List<HotItemRankingDTO> hot();

        Document userReport(long targetUserId);

        StatisticsReportDTO dashboard(int year, int month);

        void setStatus(String message);
    }

    private record YearMonthSnapshot(String yearText, String monthText) {
        private int year() {
            return UiInputParsers.requiredInt(yearText, "年份");
        }

        private int month() {
            return UiInputParsers.requiredInt(monthText, "月份");
        }
    }
}
