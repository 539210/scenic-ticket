package com.scenicticket.ui;

import com.scenicticket.dto.AuditLogQuery;
import org.bson.Document;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Date;
import java.util.List;

public final class AuditPanel extends JPanel {
    private final UiTaskExecutor taskExecutor;
    private final Actions actions;
    private final JTextField userIdField = new JTextField(10);
    private final JTextField startDateField = new JTextField(10);
    private final JTextField endDateField = new JTextField(10);
    private final JTextField keywordField = new JTextField(12);
    private final JTextField limitField = new JTextField("80", 5);
    private final JComboBox<String> logTypeBox = new JComboBox<>(
            new String[]{"全部类型", "登录", "退出", "注册", "创建订单", "景点更新", "查看报表"});
    private final JComboBox<String> levelBox = new JComboBox<>(
            new String[]{"全部级别", "正常", "警告", "错误"});
    private final DefaultTableModel logModel = tableModel("时间", "用户ID", "类型", "级别", "内容", "操作", "IP地址");
    private final DefaultTableModel summaryModel = tableModel("类型", "级别", "操作次数", "涉及用户", "最近时间");
    private final DefaultTableModel trendModel = tableModel("日期", "类型", "级别", "次数");
    private final DefaultTableModel userSummaryModel = tableModel("用户ID", "操作次数", "警告", "错误", "最近时间", "操作类型");
    private final JScrollPane logScroll = UiComponents.scroll(UiComponents.table(logModel));
    private final JScrollPane summaryScroll = UiComponents.scroll(UiComponents.table(summaryModel));
    private final JScrollPane trendScroll = UiComponents.scroll(UiComponents.table(trendModel));
    private final JScrollPane userSummaryScroll = UiComponents.scroll(UiComponents.table(userSummaryModel));
    private final JTabbedPane auditTabs = new JTabbedPane(JTabbedPane.TOP);

    public AuditPanel(UiTaskExecutor taskExecutor, Actions actions) {
        super(new BorderLayout(12, 12));
        this.taskExecutor = taskExecutor;
        this.actions = actions;
        setBackground(UiTheme.BACKGROUND);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        auditTabs.addTab("日志明细", logScroll);
        auditTabs.addTab("审计汇总", summaryScroll);
        auditTabs.addTab("审计趋势", trendScroll);
        auditTabs.addTab("用户操作", userSummaryScroll);
        add(UiComponents.card("审计条件", createToolbar()), BorderLayout.NORTH);
        add(UiComponents.card("审计结果", auditTabs), BorderLayout.CENTER);
    }

    private JPanel createToolbar() {
        JPanel filters = UiComponents.toolbar();
        JPanel moreFilters = UiComponents.toolbar();
        JPanel buttons = UiComponents.toolbar();
        JPanel toolbar = new JPanel(new GridLayout(3, 1, 0, 4));
        toolbar.setOpaque(false);
        JButton queryButton = UiComponents.primaryButton("查询日志");
        JButton summaryButton = UiComponents.secondaryButton("审计汇总");
        JButton trendButton = UiComponents.secondaryButton("审计趋势");
        JButton userSummaryButton = UiComponents.secondaryButton("用户操作");
        JButton refreshButton = UiComponents.secondaryButton("刷新当前结果");
        JButton clearButton = UiComponents.secondaryButton("清空条件");

        filters.add(new JLabel("用户ID"));
        filters.add(userIdField);
        filters.add(new JLabel("类型"));
        filters.add(logTypeBox);
        filters.add(new JLabel("级别"));
        filters.add(levelBox);
        moreFilters.add(new JLabel("开始日期"));
        moreFilters.add(startDateField);
        moreFilters.add(new JLabel("结束日期"));
        moreFilters.add(endDateField);
        moreFilters.add(new JLabel("关键词"));
        moreFilters.add(keywordField);
        moreFilters.add(new JLabel("条数"));
        moreFilters.add(limitField);
        buttons.add(queryButton);
        buttons.add(summaryButton);
        buttons.add(trendButton);
        buttons.add(userSummaryButton);
        buttons.add(refreshButton);
        buttons.add(clearButton);
        toolbar.add(filters);
        toolbar.add(moreFilters);
        toolbar.add(buttons);

        queryButton.addActionListener(event -> refreshAuditLogs());
        summaryButton.addActionListener(event -> loadSummary());
        trendButton.addActionListener(event -> loadTrend());
        userSummaryButton.addActionListener(event -> loadUserSummary());
        refreshButton.addActionListener(event -> refreshCurrentTab());
        clearButton.addActionListener(event -> clearFilters());
        userIdField.addActionListener(event -> refreshAuditLogs());
        keywordField.addActionListener(event -> refreshAuditLogs());
        return toolbar;
    }

    private void refreshAuditLogs() {
        FilterSnapshot snapshot = filterSnapshot();
        taskExecutor.run("审计日志查询", () -> actions.query(snapshot.toQuery()), documents -> {
            List<Document> safeDocuments = safeDocuments(documents);
            int scrollPosition = logScroll.getVerticalScrollBar().getValue();
            logModel.setRowCount(0);
            for (Document document : safeDocuments) {
                Document detail = document.get("action_detail", Document.class);
                logModel.addRow(new Object[]{UiFormatters.date(document.get("timestamp")), valueText(document.get("user_id")),
                        logTypeName(document.getString("log_type")), logLevelName(document.getString("log_level")),
                        valueText(document.get("message")), detail == null ? "-" : valueText(detail.get("operation")),
                        detail == null ? "-" : valueText(detail.get("ip"))});
            }
            auditTabs.setSelectedIndex(0);
            restoreScroll(logScroll, scrollPosition);
            actions.setStatus("查询到 " + safeDocuments.size() + " 条审计日志");
        });
    }

    private void loadSummary() {
        DateRangeSnapshot snapshot = dateRangeSnapshot();
        taskExecutor.run("审计汇总", () -> actions.summary(snapshot.startTime(), snapshot.endTime()), documents -> {
            int scrollPosition = summaryScroll.getVerticalScrollBar().getValue();
            summaryModel.setRowCount(0);
            for (Document document : safeDocuments(documents)) {
                summaryModel.addRow(new Object[]{logTypeName(document.getString("log_type")),
                        logLevelName(document.getString("log_level")), numberText(document.get("operation_count")),
                        numberText(document.get("user_count")), UiFormatters.date(document.get("latest_timestamp"))});
            }
            auditTabs.setSelectedIndex(1);
            restoreScroll(summaryScroll, scrollPosition);
        });
    }

    private void loadTrend() {
        DateRangeSnapshot snapshot = dateRangeSnapshot();
        taskExecutor.run("审计趋势", () -> actions.trend(snapshot.startTime(), snapshot.endTime()), documents -> {
            int scrollPosition = trendScroll.getVerticalScrollBar().getValue();
            trendModel.setRowCount(0);
            for (Document document : safeDocuments(documents)) {
                trendModel.addRow(new Object[]{valueText(document.get("date")),
                        logTypeName(document.getString("log_type")), logLevelName(document.getString("log_level")),
                        numberText(document.get("operation_count"))});
            }
            auditTabs.setSelectedIndex(2);
            restoreScroll(trendScroll, scrollPosition);
        });
    }

    private void loadUserSummary() {
        String startText = startDateField.getText();
        String endText = endDateField.getText();
        String limitText = limitField.getText();
        taskExecutor.run("用户操作汇总", () -> actions.userSummary(
                UiInputParsers.optionalStartDate(startText, "开始日期"),
                UiInputParsers.optionalEndDate(endText, "结束日期"),
                UiInputParsers.optionalInt(limitText, 50, "条数")), documents -> {
            int scrollPosition = userSummaryScroll.getVerticalScrollBar().getValue();
            userSummaryModel.setRowCount(0);
            for (Document document : safeDocuments(documents)) {
                userSummaryModel.addRow(new Object[]{valueText(document.get("user_id")),
                        numberText(document.get("operation_count")), numberText(document.get("warn_count")),
                        numberText(document.get("error_count")), UiFormatters.date(document.get("latest_timestamp")),
                        logTypeListText(document.get("log_types"))});
            }
            auditTabs.setSelectedIndex(3);
            restoreScroll(userSummaryScroll, scrollPosition);
        });
    }

    private void refreshCurrentTab() {
        switch (auditTabs.getSelectedIndex()) {
            case 0 -> refreshAuditLogs();
            case 1 -> loadSummary();
            case 2 -> loadTrend();
            case 3 -> loadUserSummary();
            default -> refreshAuditLogs();
        }
    }

    private void clearFilters() {
        userIdField.setText("");
        startDateField.setText("");
        endDateField.setText("");
        keywordField.setText("");
        limitField.setText("80");
        logTypeBox.setSelectedIndex(0);
        levelBox.setSelectedIndex(0);
        refreshAuditLogs();
    }

    private FilterSnapshot filterSnapshot() {
        return new FilterSnapshot(userIdField.getText(), logTypeBox.getSelectedIndex(), levelBox.getSelectedIndex(),
                startDateField.getText(), endDateField.getText(), keywordField.getText(), limitField.getText());
    }

    private DateRangeSnapshot dateRangeSnapshot() {
        String startText = startDateField.getText();
        String endText = endDateField.getText();
        return new DateRangeSnapshot(startText, endText);
    }

    private static String selectedLogType(int index) {
        return switch (index) {
            case 1 -> "LOGIN";
            case 2 -> "LOGOUT";
            case 3 -> "REGISTER";
            case 4 -> "ORDER_CREATE";
            case 5 -> "ITEM_UPDATE";
            case 6 -> "REPORT_VIEW";
            default -> null;
        };
    }

    private static String selectedLogLevel(int index) {
        return switch (index) {
            case 1 -> "INFO";
            case 2 -> "WARN";
            case 3 -> "ERROR";
            default -> null;
        };
    }

    private static String logTypeName(String logType) {
        if (logType == null || logType.isBlank()) {
            return "-";
        }
        return switch (logType) {
            case "LOGIN" -> "登录";
            case "LOGOUT" -> "退出";
            case "REGISTER" -> "注册";
            case "ORDER_CREATE", "ORDER" -> "创建订单";
            case "ITEM_UPDATE" -> "景点更新";
            case "REPORT_VIEW" -> "查看报表";
            default -> logType;
        };
    }

    private static String logLevelName(String level) {
        if (level == null || level.isBlank()) {
            return "-";
        }
        return switch (level) {
            case "INFO" -> "正常";
            case "WARN" -> "警告";
            case "ERROR" -> "错误";
            default -> level;
        };
    }

    private static String logTypeListText(Object value) {
        if (value instanceof List<?> values) {
            return values.stream().map(item -> logTypeName(String.valueOf(item))).distinct()
                    .reduce((left, right) -> left + "、" + right).orElse("-");
        }
        return valueText(value);
    }

    private static String valueText(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static String numberText(Object value) {
        return value instanceof Number number ? String.valueOf(number.longValue()) : valueText(value);
    }

    private static List<Document> safeDocuments(List<Document> documents) {
        return documents == null ? List.of() : documents;
    }

    private static void restoreScroll(JScrollPane scrollPane, int position) {
        javax.swing.SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(position));
    }

    private static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    void setFilters(String userId, int logTypeIndex, int levelIndex, String startDate,
                    String endDate, String keyword, String limit) {
        userIdField.setText(userId);
        logTypeBox.setSelectedIndex(logTypeIndex);
        levelBox.setSelectedIndex(levelIndex);
        startDateField.setText(startDate);
        endDateField.setText(endDate);
        keywordField.setText(keyword);
        limitField.setText(limit);
    }

    int selectedTab() {
        return auditTabs.getSelectedIndex();
    }

    int rowCount(int tabIndex) {
        return switch (tabIndex) {
            case 0 -> logModel.getRowCount();
            case 1 -> summaryModel.getRowCount();
            case 2 -> trendModel.getRowCount();
            case 3 -> userSummaryModel.getRowCount();
            default -> throw new IllegalArgumentException("未知审计页签");
        };
    }

    Object tableValueAt(int tabIndex, int row, int column) {
        return switch (tabIndex) {
            case 0 -> logModel.getValueAt(row, column);
            case 1 -> summaryModel.getValueAt(row, column);
            case 2 -> trendModel.getValueAt(row, column);
            case 3 -> userSummaryModel.getValueAt(row, column);
            default -> throw new IllegalArgumentException("未知审计页签");
        };
    }

    public interface Actions {
        List<Document> query(AuditLogQuery query);

        List<Document> summary(Date startTime, Date endTime);

        List<Document> trend(Date startTime, Date endTime);

        List<Document> userSummary(Date startTime, Date endTime, int limit);

        void setStatus(String message);
    }

    private record FilterSnapshot(String userId, int logTypeIndex, int levelIndex,
                                  String startDate, String endDate, String keyword, String limit) {
        private AuditLogQuery toQuery() {
            AuditLogQuery query = new AuditLogQuery();
            query.setUserId(UiInputParsers.optionalLong(userId));
            query.setLogType(selectedLogType(logTypeIndex));
            query.setLogLevel(selectedLogLevel(levelIndex));
            query.setStartTime(UiInputParsers.optionalStartDate(startDate, "开始日期"));
            query.setEndTime(UiInputParsers.optionalEndDate(endDate, "结束日期"));
            query.setKeyword(keyword);
            query.setLimit(UiInputParsers.optionalInt(limit, 80, "条数"));
            return query;
        }
    }

    private record DateRangeSnapshot(String startDate, String endDate) {
        private Date startTime() {
            return UiInputParsers.optionalStartDate(startDate, "开始日期");
        }

        private Date endTime() {
            return UiInputParsers.optionalEndDate(endDate, "结束日期");
        }
    }
}
